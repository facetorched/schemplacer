package com.facetorched.schemplacer.schematic;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.util.CommandBlockUtil;
import com.mojang.brigadier.Command;
import com.sk89q.worldedit.math.BlockVector3;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** Common entry point used by commands and item events to enqueue tasks. */
public class SchematicTaskScheduler {
	public static int enqueuePlace(ServerCommandSource source, String filename, BlockVector3 pastePos, Boolean ignoreAir, Boolean remove) {
		if (CommandBlockUtil.isCommandBlockSource(source)) {
        	CommandBlockUtil.setCommandBlockSuccess(source, 0);
        }
		ISchematicTask task = new SchematicPlaceTask(source, filename, pastePos, ignoreAir, remove);
        boolean queueSuccess = SchemPlacerMod.enqueue(task);
        if (!queueSuccess) {
        	if (SchemPlacerMod.CONFIG.commandOutput)
        		source.sendFeedback(() -> Text.literal("Already " + (remove ? "removing " : "placing ") + filename), true);
			return 0;
		}
        if (SchemPlacerMod.CONFIG.commandOutput)
        	source.sendFeedback(() -> Text.literal((remove ? "Removing " : "Placing ") + filename), true);
        if (CommandBlockUtil.isCommandBlockSource(source)) {
        	return 0; // TODO test if this is necessary
        }
        return Command.SINGLE_SUCCESS;
    }
    
    public static int enqueueAnimation(ServerCommandSource source, String filenamePattern, BlockVector3 pastePos,
			Integer ticksPerFrame, Integer start, Integer end, Integer step, Boolean loop,
			Boolean removeWhenDone, Boolean clearPrevFrame, Boolean ignoreAir, boolean stop) {
		if (CommandBlockUtil.isCommandBlockSource(source)) {
			CommandBlockUtil.setCommandBlockSuccess(source, 0);
		}
		ISchematicTask task = new SchematicAnimationTask(source, filenamePattern, pastePos,
				ticksPerFrame, start, end, step, loop, removeWhenDone, clearPrevFrame, ignoreAir);
		boolean isQueued = SchemPlacerMod.isTaskQueued(task);
		if (isQueued) { // stop or toggle pause existing task
			task = SchemPlacerMod.findTask(task);
			if (task == null) { // should never happen
				source.sendError(Text.literal("Internal Error: task was queued but could not be found " + filenamePattern));
			} else if (stop) {
				if (SchemPlacerMod.CONFIG.commandOutput)
					source.sendFeedback(() -> Text.literal("Stopping animation " + filenamePattern), true);
				task.stop();
				if (task.isPaused()) task.togglePause(); // unpause to allow stopping
			} else {
				boolean paused = task.togglePause();
				if (SchemPlacerMod.CONFIG.commandOutput)
					source.sendFeedback(() -> Text.literal((paused ? "Paused animation " : "Resumed animation ") + filenamePattern), true);
			}
		} else {
			if (stop) {
				source.sendFeedback(() -> Text.literal("No existing animation to stop " + filenamePattern), true);
				return 0;
			}
			boolean queueSuccess = SchemPlacerMod.enqueue(task);
	        if (!queueSuccess) {
	        	source.sendError(Text.literal("Unexpected Error queuing animation " + filenamePattern));
				return 0;
			}
	        if (SchemPlacerMod.CONFIG.commandOutput)
				source.sendFeedback(() -> Text.literal("Playing animation " + filenamePattern), true);
		}
		if (CommandBlockUtil.isCommandBlockSource(source)) {
			return 0; // TODO test if this is necessary
		}
		return Command.SINGLE_SUCCESS;
    }
}
