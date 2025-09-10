package com.facetorched.schemplacer.schematic;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.facetorched.schemplacer.util.CommandBlockUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Batch task that iterates the clipboard region and sets blocks via WorldEdit EditSession,
 * avoiding main-thread stalls by placing a maximum of 'batchSize' blocks.
 * 
 * For PLACE: uses EditSession#setBlock for each block (optionally ignoring air).
 * For REMOVE: sets air where the schematic has non-air blocks.
 */
public class SchematicBatchTask {
    public enum Mode { PLACE, REMOVE }

    private final World weWorld;
    private final CompletableFuture<Clipboard> clipboardFuture;
    private final BlockVector3 pastePos;
    private final Mode mode;
    private final boolean ignoreAir;
    private final ServerCommandSource notifySource;
    private Clipboard clipboard;

    private BlockVector3 min;
    private BlockVector3 max;
    private BlockVector3 origin;

    private int x, y, z;
    private boolean done = false;

    public SchematicBatchTask(
	    World weWorld,
	    CompletableFuture<Clipboard> clipboardFuture,
	    BlockVector3 pastePos,
	    Mode mode,
	    boolean ignoreAir,
	    ServerCommandSource notifySource) {
	    this.weWorld = weWorld;
	    this.clipboardFuture = clipboardFuture;
	    this.pastePos = pastePos;
	    this.mode = mode;
	    this.ignoreAir = ignoreAir;
	    this.notifySource = notifySource;
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
        if (CommandBlockUtil.isCommandBlockSource(notifySource)) {
			CommandBlockUtil.setCommandBlockSuccess(notifySource, 0); // for some reason we have to do this every tick
		}
        if (!clipboardFuture.isDone()) return batchSize; // not loaded yet
        if (clipboard == null) {
	        try {
		        initClipboard();
			} catch (Exception e) {
				if (notifySource != null)
					notifySource.sendError(Text.literal("Error loading schematic: " + e.getMessage()));
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
	                if (mode == Mode.REMOVE) {
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
	            	if (notifySource != null)
	            		notifySource.sendError(Text.literal("Error setting block at " + worldPos + ": " + e.getMessage()));
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
    
    public void sendMessage(String message) {
    	ServerPlayerEntity player = notifySource.getPlayer();
		if (player != null) {
			player.sendMessage(Text.literal(message), false);
		} else if (notifySource != null) {
			notifySource.sendFeedback(() -> Text.literal(message), false);
		}
	}

    private void reportSuccess() {
        if (notifySource == null) return;
        if (CommandBlockUtil.isCommandBlockSource(notifySource)) {
            CommandBlockUtil.setCommandBlockSuccess(notifySource, 1);
        }
    }
    
    @Override
    public int hashCode() {
		return Objects.hash(weWorld, pastePos, mode, ignoreAir, notifySource);
	}
    
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SchematicBatchTask other = (SchematicBatchTask) obj;
		return Objects.equals(weWorld, other.weWorld) && Objects.equals(pastePos, other.pastePos)
				&& mode == other.mode && ignoreAir == other.ignoreAir
				&& Objects.equals(notifySource, other.notifySource);
	}
    
}