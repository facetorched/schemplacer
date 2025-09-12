package com.facetorched.schemplacer.schematic;

import com.facetorched.schemplacer.util.CommandBlockUtil;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.concurrent.CompletableFuture;

public class SchematicBatchTask {
    // Core
    private final ServerWorld mcWorld;
    private final ServerCommandSource source;
    private final String filename;
    private final boolean remove;
    private final boolean ignoreAir;
    private final BlockVector3 pastePos;
    private final CompletableFuture<Clipboard> clipboardFuture;
    private Clipboard clipboard;

    private boolean done = false;

    // Bounds (world coordinates)
    private BlockVector3 origin; // clipboard origin
    private BlockVector3 min;    // world-space min of paste region
    private BlockVector3 max;    // world-space max of paste region

    // Iteration state (block level, constrained by section bounds and overall bounds)
    private int curX, curY, curZ; // current world block position
    private ChunkSectionPos curSec; // current section
    
    // Minimum and maximum within the current section
    private BlockVector3 minPosSec; // world-space min within current section
    private BlockVector3 maxPosSec; // world-space max within current section
    
    // Minimum and maximum section coordinates (inclusive)
    private ChunkSectionPos minSec;
    private ChunkSectionPos maxSec;

    // Per-section changed local positions (0..4095 packed as short)
    private final ShortSet changedLocalPositions = new ShortOpenHashSet(512);

    public SchematicBatchTask(ServerCommandSource source,
                              String filename,
                              BlockVector3 pastePos,
                              boolean remove,
                              boolean ignoreAir) {
        this.source = source;
        this.filename = filename;
        this.clipboardFuture = SchematicService.loadClipboardSafe(source, filename);
        this.clipboardFuture.whenComplete((cb, ex) -> {
			if (ex != null) {
				if (source != null)
					source.sendError(Text.literal("Error loading schematic: " + ex.getMessage()));
				done = true;
			}
		});
        this.pastePos = pastePos;
        this.remove = remove;
        this.ignoreAir = ignoreAir;
        this.mcWorld = source.getWorld();
    }

    private void initClipboard() {
        this.clipboard = clipboardFuture.join();
        this.origin = clipboard.getOrigin(); // WorldEdit origin inside the clipboard

        // Compute world-space bounds of the paste region
        BlockVector3 cbMin = clipboard.getMinimumPoint();
        BlockVector3 cbMax = clipboard.getMaximumPoint();
        // World position = pastePos + (clipboardLocal - origin)
        this.min = pastePos.add(cbMin.subtract(origin));
        this.max = pastePos.add(cbMax.subtract(origin));
        // Clamp to world height limits
        int worldMinY = mcWorld.getBottomY();
        int worldMaxY = mcWorld.getTopYInclusive();
        if (min.y() < worldMinY) min = BlockVector3.at(min.x(), worldMinY, min.z());
        if (max.y() > worldMaxY) max = BlockVector3.at(max.x(), worldMaxY, max.z());
        
        this.minSec = ChunkSectionPos.from(toMcPos(min));
        this.maxSec = ChunkSectionPos.from(toMcPos(max));

        // Start at world-space min
        this.curX = min.x();
        this.curY = min.y();
        this.curZ = min.z();

        // Initialize current section & its bounds
        setCurrentSection(ChunkSectionPos.from(toMcPos(min)));
    }

    public int tick(int batchSize) {
        if (isDone()) return batchSize;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
            CommandBlockUtil.setCommandBlockSuccess(source, 0);
        }
        if (!clipboardFuture.isDone()) return batchSize;
        if (clipboard == null) {
            try {
                initClipboard();
            } catch (Exception e) {
                if (source != null) source.sendError(Text.literal("Error loading schematic: " + e.getMessage()));
                done = true;
                return batchSize;
            }
        }

        int processed = 0;
        while (!isDone() && processed < batchSize) {
            // Compute clipboard local coords for this world block
            BlockVector3 clipLocal = origin.subtract(pastePos).add(BlockVector3.at(curX, curY, curZ));
            final var weBlock = clipboard.getBlock(clipLocal);
            final boolean weIsAir = weBlock.getBlockType() == BlockTypes.AIR;
            final BlockPos bp = new BlockPos(curX, curY, curZ);
            if (!weIsAir || !ignoreAir) {
            	try {
	                if (remove) {
	                    setBlockSilentAndCollect(bp, net.minecraft.block.Blocks.AIR.getDefaultState());
	                } else {
	                    setBlockSilentAndCollect(bp, toMcState(weBlock));
	                }
	                processed++;
	            } catch (Exception e) {
	                if (source != null) source.sendError(Text.literal("Error setting block at " + bp + ": " + e.getMessage()));
	            }
            }
            if (!advanceBlockWithinSection()) {
				// End of section reached; flush changes and move to next section
				flushSectionDelta();
				if (!advanceToNextSection()) {
					// End of all sections
					done = true;
					reportSuccess();
					break;
				}
			}
        }
        return batchSize - processed;
    }

    /* ------------------------- helpers ------------------------- */
    
    public boolean isDone() {
		return done;
	}

    public void reportSuccess() {
        if (source == null) return;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
            CommandBlockUtil.setCommandBlockSuccess(source, 1);
        }
        source.sendFeedback(() -> Text.literal((remove ? "Removed " : "Placed ") + filename), true);
    }

    private static BlockState toMcState(com.sk89q.worldedit.world.block.BlockState weState) {
        return com.sk89q.worldedit.fabric.FabricAdapter.adapt(weState);
    }
    
    private static BlockPos toMcPos(BlockVector3 vec) {
		return new BlockPos(vec.x(), vec.y(), vec.z());
	}

    /** Set current section and cache its block-space bounds. */
    private void setCurrentSection(ChunkSectionPos sec) {
        this.curSec = sec;
        BlockVector3 sMin = BlockVector3.at(
				ChunkSectionPos.getBlockCoord(sec.getSectionX()),
				ChunkSectionPos.getBlockCoord(sec.getSectionY()),
				ChunkSectionPos.getBlockCoord(sec.getSectionZ())
		);
        BlockVector3 sMax = sMin.add(15, 15, 15);
        
        this.minPosSec = BlockVector3.at(
				Math.max(sMin.x(), min.x()),
				Math.max(sMin.y(), min.y()),
				Math.max(sMin.z(), min.z())
		);
        this.maxPosSec = BlockVector3.at(
				Math.min(sMax.x(), max.x()),
				Math.min(sMax.y(), max.y()),
				Math.min(sMax.z(), max.z())
		);

        this.curX = minPosSec.x();
        this.curY = minPosSec.y();
        this.curZ = minPosSec.z();

        changedLocalPositions.clear();
    }

    /** Place without per-block client notification; collect local index for a section delta. */
    private void setBlockSilentAndCollect(BlockPos pos, BlockState newState) {
        BlockState old = mcWorld.getBlockState(pos);
        if (old == newState) return;

        // Omit NOTIFY_LISTENERS to avoid per-block packets; keep neighbors.
        final int flags = Block.NOTIFY_NEIGHBORS | Block.NO_REDRAW | Block.FORCE_STATE;
        mcWorld.setBlockState(pos, newState, flags, 0);
        short packed = ChunkSectionPos.packLocal(pos);
        changedLocalPositions.add(packed);
    }

    /** Advance X→Z→Y but do not cross section bounds; crossing triggers a section flush at the top of the loop. */
    private boolean advanceBlockWithinSection() {
        if (curX < maxPosSec.x()) curX++;
		else {
			curX = minPosSec.x();
			if (curZ < maxPosSec.z()) curZ++;
			else {
				curZ = minPosSec.z();
				if (curY < maxPosSec.y()) curY++;
				else {
					return false;
				}
			}
		}
        return true;
    }

    /** Move to the next section (X→Z→Y order) that intersects the schematic AABB. */
    private boolean advanceToNextSection() {
        int sx = curSec.getSectionX();
        int sy = curSec.getSectionY();
        int sz = curSec.getSectionZ();

        if (sx < maxSec.getSectionX()) sx++;
		else {
			sx = minSec.getSectionX();
			if (sz < maxSec.getSectionZ()) sz++;
			else {
				sz = minSec.getSectionZ();
				if (sy < maxSec.getSectionY()) sy++;
				else {
					return false;
				}
			}
        }
        setCurrentSection(ChunkSectionPos.from(sx, sy, sz));
        return true;
    }

    /** Send one delta packet for the section we just finished, then clear the collection. */
    private void flushSectionDelta() {
        if (changedLocalPositions.isEmpty()) return;

        ChunkPos chunkPos = curSec.toChunkPos();
        WorldChunk chunk = mcWorld.getChunk(chunkPos.x, chunkPos.z);
        ChunkSection section = chunk.getSection(curSec.getSectionY() - chunk.getBottomSectionCoord());

        ChunkDeltaUpdateS2CPacket pkt =
                new ChunkDeltaUpdateS2CPacket(curSec, changedLocalPositions, section);

        for (ServerPlayerEntity p : PlayerLookup.tracking(mcWorld, chunkPos)) {
            p.networkHandler.sendPacket(pkt);
        }

        changedLocalPositions.clear();
    }
}
