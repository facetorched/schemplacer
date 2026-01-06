package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.schematic.SchematicStreamTask;
import com.facetorched.schemplacer.util.CommandUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector3;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;

public class SchemStreamCommand {
	public static final String COMMAND_NAME = "schemstream";
	public static final String STOP_COMMAND_NAME = "schemstopstream";
	
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
		registerStream(COMMAND_NAME, false, dispatcher, access, env);
		registerStream(STOP_COMMAND_NAME, true, dispatcher, access, env);
	}
	
    public static void registerStream(String commandName, boolean stop, CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
    	Command<ServerCommandSource> startCommand = ctx -> execute(ctx, stop);
    	
    	dispatcher.register(CommandManager.literal(commandName)
            .then(CommandManager.argument("port", IntegerArgumentType.integer(0, 65535))
            	.executes(startCommand)
            	.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
            		.executes(startCommand)
            		.then(CommandManager.argument("skipLaggingFrames", BoolArgumentType.bool())
            			.executes(startCommand)
            			.then(CommandManager.argument("removeWhenDone", BoolArgumentType.bool())
                            .executes(startCommand)
                            .then(CommandManager.argument("clearPrevFrame", BoolArgumentType.bool())
                                .executes(startCommand)
                                .then(CommandManager.argument("ignoreAir", BoolArgumentType.bool())
                                    .executes(startCommand)
                                )
                            )
                        )
	                )
	            )
            )
        );
    }
    
    private static int execute(CommandContext<ServerCommandSource> ctx, boolean stop) {
        try {
            // Mandatory Argument 
            int port = IntegerArgumentType.getInteger(ctx, "port");

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
            Boolean skipLaggingFrames  = CommandUtil.getOptional(() -> BoolArgumentType.getBool(ctx, "skipLaggingFrames"));
            Boolean removeWhenDone    = CommandUtil.getOptional(() -> BoolArgumentType.getBool(ctx, "removeWhenDone"));
            Boolean clearPrevFrame         = CommandUtil.getOptional(() -> BoolArgumentType.getBool(ctx, "clearPrevFrame"));
            Boolean ignoreAir         = CommandUtil.getOptional(() -> BoolArgumentType.getBool(ctx, "ignoreAir"));

            // Create and run
            SchematicStreamTask task = new SchematicStreamTask(
                ctx.getSource(), port, pastePos, skipLaggingFrames, removeWhenDone, clearPrevFrame, ignoreAir
            );
            return task.enqueue(stop) ? 1 : 0;

        } catch (Exception e) {
            return 0;
        }
    }
}
