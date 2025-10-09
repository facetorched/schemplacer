package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.schematic.SchematicCacheLoader;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SchemReloadCacheCommand {
public static final String COMMAND_NAME = "schemreloadcache";
	
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
    	dispatcher.register(CommandManager.literal(COMMAND_NAME)
            .executes(ctx -> {
            	boolean success = SchematicCacheLoader.reloadCache(true);
            	if (success) {
					ctx.getSource().sendFeedback(() -> Text.literal("Reloading schematic cache..."), true);
				} else {
					ctx.getSource().sendError(Text.literal("Failed to reload schematic cache"));
				}
            	return success ? 1 : 0;
            })
        );
    }
}
