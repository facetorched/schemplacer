package com.facetorched.schemplacer.schematic;

import java.util.Objects;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.util.CommandBlockUtil;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SchematicWaitTask implements ISchematicTask {
	private final ServerCommandSource source;
	private final int ticksToWait;
	private final int waitId;
	private int ticksWaited = 0;
	private boolean done = false;
	private boolean paused = false;
	private boolean commandOutput = true;
	
	public SchematicWaitTask(ServerCommandSource source, Integer ticksToWait, Integer waitId) {
		this.source = source;
		this.ticksToWait = ticksToWait == null ? 0 : ticksToWait;
		this.waitId = waitId == null ? 0 : waitId;
		this.commandOutput = SchemPlacerMod.CONFIG.commandOutput && source != null;
	}

	@Override
    public boolean enqueue(boolean stop) {
		if (CommandBlockUtil.isCommandBlockSource(source)) {
			CommandBlockUtil.setCommandBlockSuccess(source, 0);
		}
		if (stop) done = true; // This task is not meant to be ticked
    	ISchematicTask task = this;
		boolean isQueued = SchemPlacerMod.isTaskQueued(task);
		if (isQueued) { // stop or toggle pause existing task
			task = SchemPlacerMod.findTask(task);
			if (task == null) { // should never happen
				if (commandOutput)
					source.sendError(Text.literal("Unexpected Error: finding wait task in queue"));
			} else if (stop) {
				if (commandOutput)
					source.sendFeedback(() -> Text.literal("Stopping wait"), true);
				task.stop();
				if (task.isPaused()) task.togglePause(); // unpause to allow stopping
			} else {
				boolean paused = task.togglePause();
				if (commandOutput)
					source.sendFeedback(() -> Text.literal((paused ? "Paused wait" : "Resumed wait")), true);
			}
		} else {
			if (stop) {
				if (commandOutput)
					source.sendFeedback(() -> Text.literal("No existing wait to stop"), true);
				return false;
			}
			boolean queueSuccess = SchemPlacerMod.enqueue(task);
	        if (!queueSuccess) {
	        	if (commandOutput)
	        		source.sendError(Text.literal("Unexpected Error queuing wait"));
				return false;
			}
	        if (commandOutput)
				source.sendFeedback(() -> Text.literal("Waiting " + ticksToWait + " ticks"), true);
		}
		if (CommandBlockUtil.isCommandBlockSource(source)) {
			return false;
		}
		return true;
    }

	@Override
	public int tick(int batchSize) {
		if (done) return batchSize;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
			CommandBlockUtil.setCommandBlockSuccess(source, 0); // for some reason we have to do this every tick
		}
        if (paused) return batchSize;
		ticksWaited++;
		if (isDone()) reportSuccess();
		return batchSize;
	}
	
	private void reportSuccess() {
    	done = true;
        if (source == null) return;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
            CommandBlockUtil.setCommandBlockSuccess(source, 1);
        }
        if (commandOutput)
        	source.sendFeedback(() -> Text.literal(("Waited " + ticksToWait + " ticks")), true);
    }

	@Override
	public boolean togglePause() {
		paused = !paused;
		return paused;
	}

	@Override
	public boolean isPaused() {
		return paused;
	}

	@Override
	public void stop() {
		done = true;
	}

	@Override
	public boolean isDone() {
		return ticksWaited >= ticksToWait || done;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(ticksToWait, waitId);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof SchematicWaitTask other)) return false;
		return ticksToWait == other.ticksToWait && waitId == other.waitId;
	}

}
