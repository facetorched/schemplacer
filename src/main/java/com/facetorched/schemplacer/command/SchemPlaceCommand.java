package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.schematic.SchematicPlaceTask;
import com.facetorched.schemplacer.util.CommandUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector3;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;

public class SchemPlaceCommand {
	public static final String COMMAND_NAME = "schemplace";
	public static final String REMOVE_COMMAND_NAME = "schemremove";
	
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
		registerPlace(COMMAND_NAME, false, dispatcher, access, env);
		registerPlace(REMOVE_COMMAND_NAME, true, dispatcher, access, env);
	}
	
	public static void registerPlace(String commandName, boolean remove, CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
    	Command<ServerCommandSource> startCommand = ctx -> execute(ctx, remove);
        
    	dispatcher.register(CommandManager.literal(commandName)
            .then(CommandManager.argument("filename", StringArgumentType.string())
        		.executes(startCommand)
				.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
					.executes(startCommand)
					.then(CommandManager.argument("ignoreAir", BoolArgumentType.bool())
						.executes(startCommand)
					)
				)
			)
        );
    }
    
    private static int execute(CommandContext<ServerCommandSource> ctx, boolean remove) {
        try {
            // Mandatory Argument 
            String filename = StringArgumentType.getString(ctx, "filename");

            // Optional Arguments
            BlockPos mcPos = CommandUtil.getOptional(() -> {
				try {
					return BlockPosArgumentType.getLoadedBlockPos(ctx, "pos");
				} catch (CommandSyntaxException e) {
					throw new IllegalArgumentException();
				}
			});
            if (mcPos == null) mcPos = BlockPos.ofFloored(ctx.getSource().getPosition());
            BlockVector3 pastePos = FabricAdapter.adapt(mcPos);
            Boolean ignoreAir  = CommandUtil.getOptional(() -> BoolArgumentType.getBool(ctx, "ignoreAir"));

            // Create and run
            SchematicPlaceTask task = new SchematicPlaceTask(
                ctx.getSource(), filename, pastePos, ignoreAir, remove
            );
            return task.enqueue(remove) ? 1 : 0;

        } catch (Exception e) {
            return 0;
        }
    }
}