package com.facetorched.schemplacer.schematic;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.util.CommandBlockUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Batch task that iterates the clipboard region and sets blocks via WorldEdit EditSession,
 * avoiding main-thread stalls by placing a maximum of 'batchSize' blocks.
 * 
 * For PLACE: uses EditSession#setBlock for each block (optionally ignoring air).
 * For REMOVE: sets air where the schematic has non-air blocks.
 */
public class SchematicPlaceTask implements ISchematicTask {
	private final ServerCommandSource source;
    private final String filename;
    private final BlockVector3 pastePos;
    private final boolean remove;
    private final boolean ignoreAir;
    
    private final World weWorld;
    private final CompletableFuture<Clipboard> clipboardFuture;
    private final CompletableFuture<Clipboard> removeClipboardFuture;
    
    private Clipboard clipboard;
    private BlockVector3 min;
    private BlockVector3 max;
    private BlockVector3 origin;
    private int x, y, z;
    private boolean done = false;
    private boolean paused = false;
    private boolean commandOutput = true;
    
    public SchematicPlaceTask(
    	ServerCommandSource source,
    	String filename,
    	CompletableFuture<Clipboard> clipboardFuture,
	    BlockVector3 pastePos,
	    Boolean ignoreAir,
	    Boolean remove,
	    boolean silent,
	    CompletableFuture<Clipboard> removeClipboardFuture) {
    	this.source = source;
    	this.filename = filename;
	    this.pastePos = pastePos;
	    this.ignoreAir = ignoreAir == null ? true : ignoreAir;
	    this.remove = remove == null ? false : remove;
	    if (silent) this.commandOutput = false;
	    else this.commandOutput = SchemPlacerMod.CONFIG.commandOutput && source != null;
	    this.removeClipboardFuture = removeClipboardFuture;
	    
        this.weWorld = FabricAdapter.adapt(source.getWorld());
    	this.clipboardFuture = clipboardFuture == null ? SchematicService.loadClipboardSafe(source, filename) : clipboardFuture;
    	this.clipboardFuture.whenComplete((cb, ex) -> {
    		if (cb != null && ex == null) {
    			initClipboard();
    		}
    		else {
				if (commandOutput)
					source.sendError(Text.literal("Error loading schematic: " + ex.getMessage()));
				done = true;
			}
		});
    }
    
    public SchematicPlaceTask(
		ServerCommandSource source,
		String filename,
		CompletableFuture<Clipboard> clipboardFuture,
	    BlockVector3 pastePos,
	    Boolean ignoreAir,
	    Boolean remove) {
		this(source, filename, clipboardFuture, pastePos, ignoreAir, remove, false, null);
	}
    
    public SchematicPlaceTask(
    	ServerCommandSource source,
    	String filename,
	    BlockVector3 pastePos,
	    Boolean ignoreAir,
	    Boolean remove) {
    	this(source, filename, null, pastePos, ignoreAir, remove);
    }
    
    private void initClipboard() {
    	this.clipboard = clipboardFuture.join();
        this.min = clipboard.getRegion().getMinimumPoint();
        this.max = clipboard.getRegion().getMaximumPoint();
        if (removeClipboardFuture != null) {
        	Clipboard removeClipboard = null;
			try {
				removeClipboard = removeClipboardFuture.join();
			} catch (Exception e) {
				if (commandOutput)
            		source.sendError(Text.literal("Error loading remove schematic: " + e.getMessage()));
				done = true;
				return;
			}
			this.min = this.min.getMinimum(removeClipboard.getRegion().getMinimumPoint());
			this.max = this.max.getMaximum(removeClipboard.getRegion().getMaximumPoint());
		}
        this.origin = clipboard.getOrigin();
        this.x = min.x();
        this.y = min.y();
        this.z = min.z();
	}
    
    @Override
    public boolean enqueue(boolean unused) {
		if (CommandBlockUtil.isCommandBlockSource(source)) {
        	CommandBlockUtil.setCommandBlockSuccess(source, 0);
        }
		
        boolean queueSuccess = SchemPlacerMod.enqueue(this);
        if (!queueSuccess) {
        	if (SchemPlacerMod.CONFIG.commandOutput)
        		source.sendFeedback(() -> Text.literal("Already " + (remove ? "removing " : "placing ") + filename), true);
			return false;
		}
        if (SchemPlacerMod.CONFIG.commandOutput)
        	source.sendFeedback(() -> Text.literal((remove ? "Removing " : "Placing ") + filename), true);
        if (CommandBlockUtil.isCommandBlockSource(source)) {
        	return false;
        }
        return true;
    }
    
    @Override
    public boolean isDone() { return done; }
    
    @Override
    public boolean togglePause() { return paused = !paused; }
    
    @Override
    public boolean isPaused() { return paused; }
    
    @Override
    public void stop() { done = true; }
    
    @Override
    public int tick(int batchSize) {
        if (done) return batchSize;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
			CommandBlockUtil.setCommandBlockSuccess(source, 0); // for some reason we have to do this every tick
		}
        if (paused) return batchSize;
        if (!clipboardLoaded()) return batchSize; // not loaded yet
        int processed = 0;
        Clipboard removeClipboard = null;
        if (removeClipboardFuture != null) {
			removeClipboard = removeClipboardFuture.join();
		}
        try (EditSession edit = WorldEdit.getInstance().newEditSession(weWorld)) {
        	edit.setSideEffectApplier(SideEffectSet.defaults()
        			.with(SideEffect.LIGHTING, SideEffect.State.DELAYED)
        			.with(SideEffect.NEIGHBORS, SideEffect.State.OFF)
        			.with(SideEffect.UPDATE, SideEffect.State.OFF));
            while (!done && processed < batchSize) {
                BlockVector3 c = BlockVector3.at(x, y, z);
                var schemBlock = clipboard.getBlock(c);
                boolean isAir = schemBlock.getBlockType().id().equals("minecraft:air");
                BlockVector3 worldPos = pastePos.add(c.subtract(origin));
                
                if (isAir && ignoreAir) { // usually we just skip, unless removing a removeClipboard
                	if (removeClipboard != null && !removeClipboard.getBlock(c).getBlockType().id().equals("minecraft:air")) {
						try {
							edit.setBlock(worldPos, BlockTypes.AIR.getDefaultState());
							processed++;
						} catch (Exception e) {
							if (commandOutput)
	    	            		source.sendError(Text.literal("Error setting block at " + worldPos + ": " + e.getMessage()));
						}
					}
                }
                else {
                	try {
		                if (remove) {
		                    edit.setBlock(worldPos, BlockTypes.AIR.getDefaultState());
		                } else { // PLACE
	                        edit.setBlock(worldPos, schemBlock);
		                }
		                processed++;
                	} catch (Exception e) {
    	            	if (commandOutput)
    	            		source.sendError(Text.literal("Error setting block at " + worldPos + ": " + e.getMessage()));
    	            }
                }
                
                if (!advance()) reportSuccess();
            }
        }
        return batchSize - processed;
    }

    private boolean advance() {
        x++;
        if (x > max.x()) {
            x = min.x();
            z++;
            if (z > max.z()) {
                z = min.z();
                y++;
                if (y > max.y()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void reportSuccess() {
    	done = true;
        if (source == null) return;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
            CommandBlockUtil.setCommandBlockSuccess(source, 1);
        }
        if (commandOutput)
        	source.sendFeedback(() -> Text.literal((remove ? "Removed " : "Placed ") + filename), true);
    }
    
    public boolean clipboardLoaded() {
		return clipboard != null;
	}
    
    public boolean clipboardErrored() {
    	return clipboardFuture.isCompletedExceptionally();
    }
    
    public Exception getClipboardError() {
		if (!clipboardErrored()) return null;
		try {
			clipboardFuture.join();
		} catch (Exception e) {
			return e;
		}
		return null;
	}
    
    public Clipboard getClipboard() {
		if (!clipboardLoaded()) return null;
		return clipboard;
	}
    
    public CompletableFuture<Clipboard> getClipboardFuture() {
    	return clipboardFuture;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(weWorld, filename, pastePos, remove, ignoreAir);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SchematicPlaceTask other)) return false;
        return Objects.equals(weWorld, other.weWorld)
        	&& Objects.equals(filename, other.filename)
            && Objects.equals(pastePos, other.pastePos)
            && remove == other.remove
            && ignoreAir == other.ignoreAir;
    }
}