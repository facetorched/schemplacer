package com.facetorched.schemplacer.schematic;

import java.util.List;
import java.util.Locale;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.command.SchemAnimateCommand;
import com.facetorched.schemplacer.command.SchemPlaceCommand;
import com.facetorched.schemplacer.command.SchemRemoveCommand;
import com.facetorched.schemplacer.command.SchemStopAnimateCommand;
import com.facetorched.schemplacer.command.SchemStopWaitCommand;
import com.facetorched.schemplacer.command.SchemWaitCommand;
import com.facetorched.schemplacer.util.CommandBlockUtil;
import com.facetorched.schemplacer.util.UnknownSchemCommandException;
import com.sk89q.worldedit.math.BlockVector3;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SchematicSequenceTask implements ISchematicTask {
	private final ServerCommandSource source;
	private final ISchematicTask [] tasks;
	private final boolean [] isStop;
	private int currentTaskIndex = 0;
	private boolean done = false;
	private boolean paused = false;
	private boolean stopped = false;
	private boolean commandOutput = true;
	
	public SchematicSequenceTask(ServerCommandSource source, String taskString) throws UnknownSchemCommandException {
		this(source, taskString.split("\\\\n|,"));
	}
	
	public SchematicSequenceTask(ServerCommandSource source, String [] taskStrings) throws UnknownSchemCommandException {
		this.source = source;
		this.commandOutput = SchemPlacerMod.CONFIG.commandOutput && source != null;
		if (taskStrings == null || taskStrings.length == 0) {
			throw new IllegalArgumentException("No commands in task list");
		}
		this.tasks = new ISchematicTask[taskStrings.length];
		this.isStop = new boolean[taskStrings.length]; // initialized to false
		for (int i = 0; i < taskStrings.length; i++) {
			String line = taskStrings[i].trim();
			if (line.isEmpty()) throw new IllegalArgumentException("Empty command in task list");
	        String[] toks = line.split("\\s+");
	        if (toks.length < 1) throw new IllegalArgumentException("Empty command in task list");
	        String cmd = toks[0].toLowerCase(Locale.ROOT);
	        if (cmd.startsWith("/")) cmd = cmd.substring(1);
	        
	        ISchematicTask task = null;
	        if (cmd.equals(SchemPlaceCommand.COMMAND_NAME)) {
	        	task = parsePlaceTask(false, toks);
	        } else if (cmd.equals(SchemRemoveCommand.COMMAND_NAME)) {
	        	task = parsePlaceTask(true, toks);
	        } else if (cmd.equals(SchemAnimateCommand.COMMAND_NAME) || cmd.equals(SchemStopAnimateCommand.COMMAND_NAME)) {
	        	task = parseAnimationTask(toks);
	        	isStop[i] = cmd.equals(SchemStopAnimateCommand.COMMAND_NAME);
	        } else if (cmd.equals(SchemWaitCommand.COMMAND_NAME) || cmd.equals(SchemStopWaitCommand.COMMAND_NAME)) {
	        	task = parseWaitTask(toks);
	        	isStop[i] = cmd.equals(SchemStopWaitCommand.COMMAND_NAME);
			} else {
				throw new UnknownSchemCommandException("Unknown command in task list: " + cmd);
			}
	        if (task == null) {
	        	throw new IllegalArgumentException("Failed to parse command in task list: " + line);
	        }
	        this.tasks[i] = task;
		}
	}

	@Override
	public int tick(int batchSize) {
		if (done) return batchSize;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
			CommandBlockUtil.setCommandBlockSuccess(source, 0); // for some reason we have to do this every tick
		}
        if (paused) return batchSize;
		if (currentTaskIndex >= tasks.length) {
			done = true;
			return batchSize;
		}
		ISchematicTask currentTask = tasks[currentTaskIndex];
		if (isStop[currentTaskIndex]) {
			if (stopped) {
				done = true;
				if (commandOutput)
					source.sendFeedback(() -> Text.literal("Completed sequence"), true);
				return batchSize;
			}
			currentTask.enqueue(true);
			currentTaskIndex++;
			if (currentTaskIndex >= tasks.length) {
				done = true;
				if (commandOutput)
					source.sendFeedback(() -> Text.literal("Completed sequence"), true);
				return batchSize;
			}
			currentTask = tasks[currentTaskIndex];
		}
		batchSize = currentTask.tick(batchSize);
		if (currentTask.isDone()) {
			currentTaskIndex++;
			if (currentTaskIndex >= tasks.length || stopped) {
				reportSuccess();
			}
		}
		return batchSize;
	}
	
	private void reportSuccess() {
    	done = true;
        if (source == null) return;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
            CommandBlockUtil.setCommandBlockSuccess(source, 1);
        }
        if (commandOutput)
        	source.sendFeedback(() -> Text.literal("Completed sequence"), true);
    }
	
	@Override
    public boolean enqueue(boolean stop) {
		if (tasks.length == 1) { // single task, just enqueue it
			// invert stop if it's a stop command
			if (isStop[0]) stop = !stop;
			return tasks[0].enqueue(stop);
		}
    	if (CommandBlockUtil.isCommandBlockSource(source)) {
			CommandBlockUtil.setCommandBlockSuccess(source, 0);
		}
    	if (stop) done = true; // This task is not meant to be ticked
    	ISchematicTask task = this;
		boolean isQueued = SchemPlacerMod.isTaskQueued(task);
		if (isQueued) { // stop or toggle pause existing task
			task = SchemPlacerMod.findTask(task);
			if (task == null) { // should never happen
				source.sendError(Text.literal("Unexpected Error: finding sequence in queue"));
			} else if (stop) {
				if (SchemPlacerMod.CONFIG.commandOutput)
					source.sendFeedback(() -> Text.literal("Stopping sequence"), true);
				task.stop();
				if (task.isPaused()) task.togglePause(); // unpause to allow stopping
			} else {
				boolean paused = task.togglePause();
				if (SchemPlacerMod.CONFIG.commandOutput)
					source.sendFeedback(() -> Text.literal((paused ? "Paused sequence" : "Resumed sequence")), true);
			}
		} else {
			if (stop) {
				source.sendFeedback(() -> Text.literal("No existing sequence to stop"), true);
				return false;
			}
			boolean queueSuccess = SchemPlacerMod.enqueue(task);
	        if (!queueSuccess) {
	        	source.sendError(Text.literal("Unexpected Error queuing sequence"));
				return false;
			}
	        if (SchemPlacerMod.CONFIG.commandOutput)
				source.sendFeedback(() -> Text.literal("Playing sequence"), true);
		}
		if (CommandBlockUtil.isCommandBlockSource(source)) {
			return false;
		}
		return true;
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
		if (!done && currentTaskIndex < tasks.length) {
			tasks[currentTaskIndex].stop();
			if (tasks[currentTaskIndex].isDone()) {
				currentTaskIndex++;
				done = true;
			}
		}
		stopped = true;
	}

	@Override
	public boolean isDone() {
		return done;
	}
	
	private ISchematicTask parsePlaceTask(boolean remove, String [] toks) {
    	String filename = getFilenameArg(toks, 1);
    	if (filename == null) return null;
        BlockVector3 pos = getPastePositionArg(source, toks, 2);
        if (pos == null) return null;
        
        if (source.getPlayer() != null && source.getPlayer().isSneaking()) remove = !remove;
        Boolean ignoreAir = getBooleanArg(toks, 5);
        if (ignoreAir == null) ignoreAir = true;
        return new SchematicPlaceTask(source, filename, pos, ignoreAir, remove);
	}
	
    private ISchematicTask parseAnimationTask(String[] toks) {
		String filenamePattern = getFilenameArg(toks, 1);
		if (filenamePattern == null) return null;
		BlockVector3 pos = getPastePositionArg(source, toks, 2);
		if (pos == null) return null;
		Integer ticksPerFrame = getIntegerArg(toks, 5);
		Integer start = getIntegerArg(toks, 6);
		Integer end = getIntegerArg(toks, 7);
		Integer step = getIntegerArg(toks, 8);
		Boolean loop = getBooleanArg(toks, 9);
		Boolean removeWhenDone = getBooleanArg(toks, 10);
		Boolean clearPrevFrame = getBooleanArg(toks, 11);
		Boolean ignoreAir = getBooleanArg(toks, 12);
		return new SchematicAnimationTask(source, filenamePattern, pos,
				ticksPerFrame, start, end, step, loop,
				removeWhenDone, clearPrevFrame, ignoreAir);
	}
    
	private ISchematicTask parseWaitTask(String[] toks) {
		Integer ticksToWait = getIntegerArg(toks, 1);
		if (ticksToWait == null) return null;
		Integer waitId = getIntegerArg(toks, 2);
		return new SchematicWaitTask(source, ticksToWait, waitId);
	}
    
    private static String getFilenameArg(String[] toks, int index) {
    	if (index < toks.length) {
    		String input = toks[index];
	        if (input == null || input.length() < 2) return input;
	        char first = input.charAt(0);
	        char last = input.charAt(input.length() - 1);
	        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
	            return input.substring(1, input.length() - 1);
	        }
	        return input;
    	}
    	return null;
    }
    
    private static BlockVector3 getPastePositionArg(ServerCommandSource source, String[] toks, int index) {
    	BlockVector3 pos = null;
        if (index + 2 < toks.length) { // position specified in lore
            try {
            	pos = BlockVector3.at(
            			Integer.parseInt(toks[index]),
            			Integer.parseInt(toks[index + 1]),
            			Integer.parseInt(toks[index + 2])
            	);
            } catch (NumberFormatException e) {
            	return null;
            }
        } else if (source.getPosition() != null) { // use player position
        	pos = BlockVector3.at(
					(int)Math.floor(source.getPosition().x),
					(int)Math.floor(source.getPosition().y),
					(int)Math.floor(source.getPosition().z)
			);
		} else {
			return null;
		}
        return pos;
    }
    
    private static Integer getIntegerArg(String[] toks, int index) {
		if (index < toks.length) {
			try {
				return Integer.parseInt(toks[index]);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
    
    private static Boolean getBooleanArg(String[] toks, int index) {
		if (index < toks.length) {
			String boolStr = toks[index].toLowerCase(Locale.ROOT);
			if (boolStr.equals("true")) return true;
			else if (boolStr.equals("false")) return false;
			else return null;
		}
		return null;
    }
    
    @Override 
    public int hashCode() {
		return List.of(tasks).hashCode();
	}
    
    @Override
    public boolean equals(Object obj) {
    	if (this == obj) return true;
    	if (!(obj instanceof SchematicSequenceTask other)) return false;
    	return List.of(tasks).equals(List.of(other.tasks));
    }
}
