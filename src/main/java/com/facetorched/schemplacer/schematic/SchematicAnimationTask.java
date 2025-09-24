package com.facetorched.schemplacer.schematic;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.util.FrameNumberIterator;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SchematicAnimationTask implements ISchematicTask {
    private final ServerCommandSource source;
    private final String filenamePattern;
    private final BlockVector3 pastePos;
    private final int ticksPerFrame;
    private final FrameNumberIterator frameIter;
    private final boolean clearPrevFrame;
    private final boolean removeWhenDone;
    private final boolean ignoreAir;
    
    private final World weWorld;
    private boolean commandOutput = true;

    private int ticksSinceLastFrame = 0;
    private SchematicPlaceTask currentTask = null;
    private SchematicPlaceTask nextTask = null;
    private Clipboard currentFullClipboard = null;
    private Clipboard nextFullClipboard = null;
    private boolean done = false;
    private boolean paused = false;
    private Integer frameNumber = null;
    
    public SchematicAnimationTask(
            ServerCommandSource source,
            String filenamePattern,
            BlockVector3 pastePos,
            Integer ticksPerFrame,
            Integer start,
            Integer end,
            Integer step,
            Boolean loop,
            Boolean removeWhenDone,
            Boolean clearPrevFrame,
            Boolean ignoreAir) {

        this.source = source;
        this.filenamePattern = filenamePattern;
        this.pastePos = pastePos;
        this.ticksPerFrame = ticksPerFrame != null ? Math.max(0, ticksPerFrame) : 0;
        this.frameIter = new FrameNumberIterator(start, end, step, loop); // defaults: start=0, end=null, step=1, loop=false
        this.removeWhenDone = removeWhenDone != null ? removeWhenDone : true;
        this.clearPrevFrame = clearPrevFrame != null ? clearPrevFrame : true;
        this.ignoreAir = ignoreAir != null ? ignoreAir : true;
        
        this.weWorld = FabricAdapter.adapt(source.getWorld());
        this.commandOutput = SchemPlacerMod.CONFIG.commandOutput && source != null;

        if (!this.frameIter.hasNext()) {
			if (SchemPlacerMod.CONFIG.commandOutput)
				source.sendError(Text.literal("No frames to play in animation."));
			done = true;
		}
    }

    @Override
    public int tick(int batchSize) {
        if (done || paused) return batchSize;
        if (currentTask == null) { // initialize animation
			if ((currentTask = initNextTask()) == null) {
				if (commandOutput)
					source.sendError(Text.literal("No frames to play in animation."));
				done = true;
				return batchSize;
			}
			frameNumber = frameIter.next();
			nextTask = initNextTask();
			ticksSinceLastFrame = 0;
		} else if (currentTask.clipboardErrored()) { // current task only has error if first frame failed to load
			Exception ex = currentTask.getClipboardError();
			if (commandOutput)
				source.sendError(Text.literal("Error loading first frame of animation: " + (ex != null ? ex.getMessage() : "unknown error")));
			done = true;
		}
        else { // animation in progress
        	if (nextTask != null && nextTask.clipboardErrored()) {
				if (frameIter.end == null) { // End was not specified: lack of file means end.
            		if (frameIter.loop) {
            			frameIter.reset();
            		}
            		else {
            			frameIter.stop();
            		}
            		nextTask = initNextTask();
				}
            }
			if (currentTask.clipboardLoaded())
				ticksSinceLastFrame++;
			if (currentTask.isDone()) {
				if (nextTask == null) { // no more frames: end animation
					done = true;
					if (commandOutput)
						source.sendFeedback(() -> Text.literal("Completed animation " + filenamePattern), true);
					return batchSize;
				} else if (nextTask.clipboardLoaded() && ticksSinceLastFrame >= ticksPerFrame) {
					currentTask = nextTask;
					currentFullClipboard = nextFullClipboard;
					if (frameNumber != null)
						frameNumber = frameIter.next();
					nextTask = initNextTask();
					ticksSinceLastFrame = 0;	
				}
			}
		}
        return currentTask.tick(batchSize);
    }
    
    private SchematicPlaceTask initNextTask() {
    	boolean remove = false;
    	String filename;
    	CompletableFuture<Clipboard> clipboardFuture = null;
    	if (frameIter.hasNext()) {
    		filename = buildFilename(filenamePattern, frameIter.peek());
    	} else { // No more frames. Remove last frame if needed or end animation.
            if (removeWhenDone && frameNumber != null) {
                filename = buildFilename(filenamePattern, frameNumber);
                remove = true;
                frameNumber = null; // ensure we only remove once
                clipboardFuture = CompletableFuture.completedFuture(currentFullClipboard); // use full clipboard to remove
            }
            else {
				return null;
			}
    	}
		if (clipboardFuture == null) { // not removing current frame
			clipboardFuture = CompletableFuture.supplyAsync(() -> {
				Clipboard newFullClipboard = SchematicService.loadClipboardSafe(source, filename).join();
				if (currentFullClipboard == null) {
					currentFullClipboard = newFullClipboard;
					return newFullClipboard; // first frame, nothing to clear
				}
				//} else if (nextFullClipboard != null) {
				//	currentFullClipboard = nextFullClipboard;
				//}
				nextFullClipboard = newFullClipboard;
				if (clearPrevFrame)
					return SchematicService.diffClipboard(source, currentFullClipboard, nextFullClipboard, ignoreAir);
				return nextFullClipboard;
			});
		}
        return new SchematicPlaceTask(
                source,
                filename,
                clipboardFuture,
                pastePos,
                ignoreAir,
                remove,
                true // silent
        );
    }
    
    @Override
    public boolean togglePause() {
		return paused = !paused;
	}
    
    @Override
    public boolean isPaused() {
		return paused;
	}
    
    @Override
    public void stop() {
		frameIter.stop();
		nextTask = initNextTask();
	}
    
    @Override
    public boolean isDone() {
        return done;
    }

    private static String buildFilename(String pattern, int frame) {
        if (pattern.contains("%")) {
            return String.format(pattern, frame);
        } else {
            return pattern + frame;
        }
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(weWorld, filenamePattern, pastePos, ticksPerFrame, frameIter, clearPrevFrame, removeWhenDone, ignoreAir);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SchematicAnimationTask other)) return false;
        return Objects.equals(weWorld, other.weWorld)
        	&& Objects.equals(filenamePattern, other.filenamePattern)
            && Objects.equals(pastePos, other.pastePos)
            && ticksPerFrame == other.ticksPerFrame
            && Objects.equals(frameIter, other.frameIter)
            && clearPrevFrame == other.clearPrevFrame
            && removeWhenDone == other.removeWhenDone
            && ignoreAir == other.ignoreAir;
    }
}
