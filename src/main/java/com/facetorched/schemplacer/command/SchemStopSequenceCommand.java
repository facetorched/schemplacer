package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.schematic.SchematicSequenceTask;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SchemStopSequenceCommand {
public static final String COMMAND_NAME = "schemstopsequence";
	
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
    	dispatcher.register(CommandManager.literal(COMMAND_NAME)
                .then(CommandManager.argument("line", StringArgumentType.greedyString())
                    .executes(ctx -> {
                    	SchematicSequenceTask task;
                    	try {
							task = new SchematicSequenceTask(ctx.getSource(), StringArgumentType.getString(ctx, "line"));
						} catch (Exception e) {
							if (SchemPlacerMod.CONFIG.commandOutput) {
								String msg = e != null ? e.getMessage() : "Failed to parse command sequence";
								ctx.getSource().sendError(Text.literal(msg));
							}
							return 0;
						}
                    	return task.enqueue(true) ? 1 : 0;
                    }
                )
            )
        );
    }
}