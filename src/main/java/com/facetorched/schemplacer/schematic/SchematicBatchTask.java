package com.facetorched.schemplacer.schematic;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.facetorched.schemplacer.util.CommandBlockUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;

import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/**
 * Batch task that iterates the clipboard region and sets blocks via WorldEdit EditSession,
 * avoiding main-thread stalls by placing a maximum of 'batchSize' blocks.
 * 
 * For PLACE: uses EditSession#setBlock for each block (optionally ignoring air).
 * For REMOVE: sets air where the schematic has non-air blocks.
 */
public class SchematicBatchTask {
	private final ServerCommandSource source;
    private final String filename;
    
    private final BlockVector3 pastePos;
    private final boolean remove;
    private final boolean ignoreAir;
    
    private final World weWorld;
    private final CompletableFuture<Clipboard> clipboardFuture;
    
    private Clipboard clipboard;

    private BlockVector3 min;
    private BlockVector3 max;
    private BlockVector3 origin;

    private int x, y, z;
    private boolean done = false;

    public SchematicBatchTask(
    	ServerCommandSource notifySource,
    	String filename,
	    BlockVector3 pastePos,
	    boolean remove,
	    boolean ignoreAir) {
    	this.filename = filename;
	    this.pastePos = pastePos;
	    this.remove = remove;
	    this.ignoreAir = ignoreAir;
	    this.source = notifySource;
	    ServerWorld mcWorld = notifySource.getWorld();
        this.weWorld = FabricAdapter.adapt(mcWorld);
        try {
        	this.clipboardFuture = SchematicService.loadClipboardSafe(notifySource, filename);
        } catch (Exception e) {
        	throw new RuntimeException("Error loading schematic: " + e.getMessage(), e);
        }
    }
    
    private void initClipboard() {
    	clipboard = clipboardFuture.join();
        this.min = clipboard.getRegion().getMinimumPoint();
        this.max = clipboard.getRegion().getMaximumPoint();
        this.origin = clipboard.getOrigin();
        this.x = min.x();
        this.y = min.y();
        this.z = min.z();
	}

    public int tick(int batchSize) {
        if (done) return batchSize;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
			CommandBlockUtil.setCommandBlockSuccess(source, 0); // for some reason we have to do this every tick
		}
        if (!clipboardFuture.isDone()) return batchSize; // not loaded yet
        if (clipboard == null) {
	        try {
		        initClipboard();
			} catch (Exception e) {
				if (source != null)
					source.sendError(Text.literal("Error loading schematic: " + e.getMessage()));
				done = true;
				return batchSize;
			}
		}
        int processed = 0;
        try (EditSession edit = WorldEdit.getInstance().newEditSession(weWorld)) {
            while (!done && processed < batchSize) {
                BlockVector3 c = BlockVector3.at(x, y, z);
                var schemBlock = clipboard.getBlock(c);
                boolean isAir = schemBlock.getBlockType().id().equals("minecraft:air");
                BlockVector3 worldPos = pastePos.add(c.subtract(origin));
                
                try {
	                if (remove) {
	                    if (!isAir) {
	                        edit.setBlock(worldPos, BlockTypes.AIR.getDefaultState());
	                        processed++;
	                    }
	                } else { // PLACE
	                    if (isAir && ignoreAir) {
	                        // skip air if ignoring
	                    } else {
	                        edit.setBlock(worldPos, schemBlock);
	                        processed++;
	                    }
	                }
	            } catch (Exception e) {
	            	if (source != null)
	            		source.sendError(Text.literal("Error setting block at " + worldPos + ": " + e.getMessage()));
	            }
                advance();
            }
        }
        return batchSize - processed;
    }

    private void advance() {
        x++;
        if (x > max.x()) {
            x = min.x();
            z++;
            if (z > max.z()) {
                z = min.z();
                y++;
                if (y > max.y()) {
                    done = true;
                    reportSuccess();
                }
            }
        }
    }

    public boolean isDone() { return done; }

    private void reportSuccess() {
        if (source == null) return;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
            CommandBlockUtil.setCommandBlockSuccess(source, 1);
        }
        source.sendFeedback(() -> Text.literal((remove ? "Removed " : "Placed ") + filename), true);
    }
    
    @Override
    public int hashCode() {
        // Build a stable identity for the source
        Object sourceKey = getSourceKey();
        return Objects.hash(weWorld, filename, pastePos, remove, ignoreAir, sourceKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SchematicBatchTask other)) return false;
        return Objects.equals(weWorld, other.weWorld)
        	&& Objects.equals(filename, other.filename)
            && Objects.equals(pastePos, other.pastePos)
            && remove == other.remove
            && ignoreAir == other.ignoreAir
            && Objects.equals(getSourceKey(), other.getSourceKey());
    }

    /**
     * Produce a stable identity key for the source.
     */
    private Object getSourceKey() {
        CommandBlockBlockEntity cbbe = CommandBlockUtil.getCommandBlockEntityFromSource(source);
        if (cbbe != null) {
            return cbbe.getPos(); // BlockPos is stable & equals/hashCode safe
        }
        Entity entity = source.getEntity();
        if (entity != null) {
            return entity.getUuid(); // UUID is stable across ticks
        }
        return null;
    }
}