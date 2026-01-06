package com.facetorched.schemplacer.schematic;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.util.CommandBlockUtil;
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
        this.ticksPerFrame = ticksPerFrame != null ? ticksPerFrame : 0;
        this.frameIter = new FrameNumberIterator(start, end, step, loop); // defaults: start=0, end=null, step=1, loop=false
        this.removeWhenDone = removeWhenDone != null ? removeWhenDone : true;
        this.clearPrevFrame = clearPrevFrame != null ? clearPrevFrame : true;
        this.ignoreAir = ignoreAir != null ? ignoreAir : true;
        
        this.weWorld = FabricAdapter.adapt(source.getWorld());
        this.commandOutput = SchemPlacerMod.CONFIG.commandOutput && source != null;

        if (!this.frameIter.hasNext()) {
			if (commandOutput)
				source.sendError(Text.literal("No frames to play in animation."));
			done = true;
		}
    }

    @Override
    public int tick(int batchSize) {
    	if (done) return batchSize;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
			CommandBlockUtil.setCommandBlockSuccess(source, 0); // for some reason we have to do this every tick
		}
        if (paused) return batchSize;
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
			if (currentTask.clipboardSchemNotFound() && ticksPerFrame < 0) { // wait for schematic to appear in the folder.
				currentTask.loadClipboard(); // try reloading
				return batchSize; 
			}
			Throwable ex = currentTask.getClipboardError();
			if (commandOutput) source.sendError(Text.literal("Error loading first frame of animation: " + (ex != null ? ex.getMessage() : "unknown error")));
			done = true;
		}
        else { // animation in progress
        	if (nextTask != null && nextTask.clipboardSchemNotFound()) {
        		if (ticksPerFrame < 0) { // wait for schematic to appear in the folder.
        			nextTask.loadClipboard(null, currentTask.getClipboardFuture()); // try reloading
        		}
        		else if (frameIter.end < 0) { // End not specified: lack of file means end.
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
					reportSuccess();
					return batchSize;
				} else if (nextTask.clipboardLoaded() && ticksSinceLastFrame >= ticksPerFrame) {
					currentTask = nextTask;
					if (frameNumber != null)
						frameNumber = frameIter.next();
					nextTask = initNextTask();
					ticksSinceLastFrame = 0;	
				}
			}
		}
        return currentTask.tick(batchSize);
    }
    
    private void reportSuccess() {
    	done = true;
        if (source == null) return;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
            CommandBlockUtil.setCommandBlockSuccess(source, 1);
        }
        if (commandOutput) source.sendFeedback(() -> Text.literal("Completed animation " + filenamePattern), true);
    }
    
    private SchematicPlaceTask initNextTask() {
    	boolean remove = false;
    	String filename;
    	CompletableFuture<Clipboard> clipboardFuture = null;
    	CompletableFuture<Clipboard> removeClipboardFuture = null;
    	if (frameIter.hasNext()) {
    		filename = buildFilename(filenamePattern, frameIter.peek());
    	} else { // No more frames. Remove last frame if needed or end animation.
            if (removeWhenDone && frameNumber != null) {
                filename = buildFilename(filenamePattern, frameNumber);
                remove = true;
                frameNumber = null; // ensure we only remove once
                clipboardFuture = currentTask.getClipboardFuture(); // use current clipboard to remove
            }
            else {
				return null;
			}
    	}
		if (clipboardFuture == null) { // not removing when done
			if (clearPrevFrame && currentTask != null) {
				removeClipboardFuture = currentTask.getClipboardFuture();
			}
			clipboardFuture = SchematicService.loadClipboardSafe(source, filename);
		}
        return new SchematicPlaceTask(
                source,
                filename,
                clipboardFuture,
                pastePos,
                ignoreAir,
                remove,
                true, // silent
                removeClipboardFuture
        );
    }
    
    @Override
    public boolean enqueue(boolean stop) {
    	if (CommandBlockUtil.isCommandBlockSource(source)) {
			CommandBlockUtil.setCommandBlockSuccess(source, 0);
		}
    	if (stop) done = true; // This task is not meant to be ticked
    	ISchematicTask task = this;
		boolean isQueued = SchematicTaskQueue.isTaskQueued(task);
		if (isQueued) { // stop or toggle pause existing task
			task = SchematicTaskQueue.findTask(task);
			if (task == null) { // should never happen
				if (commandOutput) source.sendError(Text.literal("Unexpected Error: finding task in queue " + filenamePattern));
			} else if (stop) {
				if (commandOutput) source.sendFeedback(() -> Text.literal("Stopping animation " + filenamePattern), true);
				task.stop();
				if (task.isPaused()) task.togglePause(); // unpause to allow stopping
			} else {
				boolean paused = task.togglePause();
				if (commandOutput) source.sendFeedback(() -> Text.literal((paused ? "Paused animation " : "Resumed animation ") + filenamePattern), true);
			}
		} else {
			if (stop) {
				if (commandOutput) source.sendFeedback(() -> Text.literal("No existing animation to stop " + filenamePattern), true);
				return false;
			}
			boolean queueSuccess = SchematicTaskQueue.enqueue(task);
	        if (!queueSuccess) {
	        	if (commandOutput) source.sendError(Text.literal("Error queuing animation (task already exists) " + filenamePattern));
				return false;
			}
	        if (commandOutput) source.sendFeedback(() -> Text.literal("Playing animation " + filenamePattern), true);
		}
		if (CommandBlockUtil.isCommandBlockSource(source)) {
			return false;
		}
		return true;
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
    	if (currentTask == null || !currentTask.clipboardLoaded()) {
    		done = true;
    		return;
    	}
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
    
    @Override
    public SchematicTaskDescription getDescription() {
		return new SchematicTaskDescription(
			"SchematicAnimationTask",
			new String[] {"filenamePattern", "pastePos", "ticksPerFrame", "start", "end", "step", "loop", "clearPrevFrame", "removeWhenDone", "ignoreAir"},
			new Object[] {filenamePattern, pastePos, ticksPerFrame, frameIter.start, frameIter.end, frameIter.step, frameIter.loop, clearPrevFrame, removeWhenDone, ignoreAir}
		);
	}
}
