package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.schematic.SchematicSequenceTask;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SchemSequenceCommand {
	public static final String COMMAND_NAME = "schemsequence";
	public static final String STOP_COMMAND_NAME = "schemstopsequence";
	
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
		registerSequence(COMMAND_NAME, false, dispatcher, access, env);
		registerSequence(STOP_COMMAND_NAME, true, dispatcher, access, env);
	}
	
    public static void registerSequence(String commandName, boolean stop, CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
    	dispatcher.register(CommandManager.literal(commandName)
            .then(CommandManager.argument("line", StringArgumentType.greedyString())
                .executes(ctx -> {
                	try {
						SchematicSequenceTask task = new SchematicSequenceTask(ctx.getSource(), StringArgumentType.getString(ctx, "line"));
						return task.enqueue(stop) ? 1 : 0;
					} catch (Exception e) {
						if (SchemPlacerMod.CONFIG.commandOutput) {
							String msg = e != null ? e.getMessage() : "Failed to parse command sequence";
							ctx.getSource().sendError(Text.literal(msg));
						}
						return 0;
					}
                	
                })
            )
        );
    }
}
