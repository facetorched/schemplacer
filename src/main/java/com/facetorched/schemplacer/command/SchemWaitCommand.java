package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.schematic.SchematicWaitTask;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class SchemWaitCommand {
	public static final String COMMAND_NAME = "schemwait";
	public static final boolean STOP = false;
	
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
    	dispatcher.register(CommandManager.literal(COMMAND_NAME)
            .then(CommandManager.argument("ticksToWait", IntegerArgumentType.integer())
                .executes(ctx -> enqueueHelper(ctx.getSource(),
						IntegerArgumentType.getInteger(ctx, "ticksToWait"),
						null))
                .then(CommandManager.argument("waitId", IntegerArgumentType.integer())
					.executes(ctx -> enqueueHelper(ctx.getSource(),
							IntegerArgumentType.getInteger(ctx, "ticksToWait"),
							IntegerArgumentType.getInteger(ctx, "waitId")))
				)
            )
        );
    }
    
    private static int enqueueHelper(ServerCommandSource source, Integer ticksToWait, Integer waitId) {
    	SchematicWaitTask task;
    	try { 
    		task = new SchematicWaitTask(source, ticksToWait, waitId);
    	} catch (Exception e) { 
			return 0; 
		}
    	return task.enqueue(STOP) ? 1 : 0;
	}
}


