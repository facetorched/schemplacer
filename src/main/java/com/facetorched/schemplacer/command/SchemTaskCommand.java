package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.schematic.ISchematicTask;
import com.facetorched.schemplacer.schematic.SchematicTaskQueue;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SchemTaskCommand {
public static final String COMMAND_NAME = "schemtask";
	
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
    	dispatcher.register(CommandManager.literal(COMMAND_NAME)
    		.then(CommandManager.literal("list")
                .executes(ctx -> {
                	if (taskQueueIsEmpty(ctx)) return 1;
                	for (ISchematicTask task : SchematicTaskQueue.getQueuedTasks()) {
                		ctx.getSource().sendFeedback(() -> task.getDescription().getTextDescription(), false);
					}
                	return 1;
                })
            )
    		.then(CommandManager.literal("stop")
				.executes(ctx -> {
					if (taskQueueIsEmpty(ctx)) return 1;
					for (ISchematicTask task : SchematicTaskQueue.getQueuedTasks()) {
						task.stop();
					}
					if (SchemPlacerMod.CONFIG.commandOutput)
						ctx.getSource().sendFeedback(() -> Text.literal("Stopping all queued schematic tasks"), true);
					return 1;
				})
			)
    		.then(CommandManager.literal("clear")
				.executes(ctx -> {
					if (taskQueueIsEmpty(ctx)) return 1;
					SchematicTaskQueue.clearQueuedTasks();
					if (SchemPlacerMod.CONFIG.commandOutput)
						ctx.getSource().sendFeedback(() -> Text.literal("Cleared all queued schematic tasks"), true);
					return 1;
				})
			)
        );
    }
    
    private static boolean taskQueueIsEmpty(CommandContext<ServerCommandSource> ctx) {
		if (SchematicTaskQueue.getQueuedTasks().length == 0) {
			if (SchemPlacerMod.CONFIG.commandOutput)
				ctx.getSource().sendFeedback(() -> Text.literal("No queued schematic tasks"), false);
			return true;
		}
		return false;
		}
}
