package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.schematic.SchematicAnimationTask;
import com.facetorched.schemplacer.util.CommandUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

public class SchemAnimateCommand {
    public static final String COMMAND_NAME = "schemanimate";
    public static final String STOP_COMMAND_NAME = "schemstopanimate";
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
    	registerAnimation(COMMAND_NAME, false, dispatcher, access, env);
    	registerAnimation(STOP_COMMAND_NAME, true, dispatcher, access, env);
    }
    	
    private static void registerAnimation(String commandName, boolean stop, CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
        Command<ServerCommandSource> startCommand = ctx -> execute(ctx, stop);

        dispatcher.register(CommandManager.literal(commandName)
            .then(CommandManager.argument("filename", StringArgumentType.string())
                .executes(startCommand)
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                    .executes(startCommand)
                    .then(CommandManager.argument("ticksPerFrame", IntegerArgumentType.integer())
                        .executes(startCommand)
                        .then(CommandManager.argument("start", IntegerArgumentType.integer())
                            .executes(startCommand)
                            .then(CommandManager.argument("end", IntegerArgumentType.integer())
                                .executes(startCommand)
                                .then(CommandManager.argument("step", IntegerArgumentType.integer())
                                    .executes(startCommand)
                                    .then(CommandManager.argument("loop", BoolArgumentType.bool())
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
                        )
                    )
                )
            )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> ctx, boolean stop) {
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
            Integer ticks   		= CommandUtil.getOptional(() -> IntegerArgumentType.getInteger(ctx, "ticksPerFrame"));
            Integer start   		= CommandUtil.getOptional(() -> IntegerArgumentType.getInteger(ctx, "start"));
            Integer end     		= CommandUtil.getOptional(() -> IntegerArgumentType.getInteger(ctx, "end"));
            Integer step    		= CommandUtil.getOptional(() -> IntegerArgumentType.getInteger(ctx, "step"));
            Boolean loop    		= CommandUtil.getOptional(() -> BoolArgumentType.getBool(ctx, "loop"));
            Boolean removeWhenDone	= CommandUtil.getOptional(() -> BoolArgumentType.getBool(ctx, "removeWhenDone"));
            Boolean clearPrevFrame 	= CommandUtil.getOptional(() -> BoolArgumentType.getBool(ctx, "clearPrevFrame"));
            Boolean ignore  		= CommandUtil.getOptional(() -> BoolArgumentType.getBool(ctx, "ignoreAir"));

            // Create and run
            SchematicAnimationTask task = new SchematicAnimationTask(
                ctx.getSource(), filename, pastePos, ticks, start, end, step, loop, removeWhenDone, clearPrevFrame, ignore
            );
            return task.enqueue(stop) ? 1 : 0;

        } catch (Exception e) {
            return 0;
        }
    }
}
