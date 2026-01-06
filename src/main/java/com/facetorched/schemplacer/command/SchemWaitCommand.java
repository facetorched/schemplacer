package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.schematic.SchematicWaitTask;
import com.facetorched.schemplacer.util.CommandUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class SchemWaitCommand {
	public static final String COMMAND_NAME = "schemwait";
	public static final String STOP_COMMAND_NAME = "schemstopwait";
	
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
		registerWait(COMMAND_NAME, false, dispatcher, access, env);
		registerWait(STOP_COMMAND_NAME, true, dispatcher, access, env);
	}
	
    public static void registerWait(String commandName, boolean stop, CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
    	Command<ServerCommandSource> startCommand = ctx -> execute(ctx, stop);
    	
    	dispatcher.register(CommandManager.literal(commandName)
            .then(CommandManager.argument("ticksToWait", IntegerArgumentType.integer())
                .executes(startCommand)
                .then(CommandManager.argument("waitId", IntegerArgumentType.integer())
					.executes(startCommand)
				)
            )
        );
    }
    
    private static int execute(CommandContext<ServerCommandSource> ctx, boolean stop) {
		try {
			// Mandatory Argument 
			int ticksToWait = IntegerArgumentType.getInteger(ctx, "ticksToWait");

			// Optional Arguments
			Integer waitId     = CommandUtil.getOptional(() -> IntegerArgumentType.getInteger(ctx, "waitId"));
			SchematicWaitTask task = new SchematicWaitTask(ctx.getSource(), ticksToWait, waitId);
			return task.enqueue(stop) ? 1 : 0;
		} catch (Exception e) {
			return 0;
		}
    }
}


