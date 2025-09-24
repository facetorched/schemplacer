package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.schematic.SchematicTaskScheduler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sk89q.worldedit.fabric.FabricAdapter;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;

public class SchemAnimateCommand {
	public static final String COMMAND_NAME = "schemanimate";
	public static final boolean STOP = false;
	
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
        dispatcher.register(CommandManager.literal(COMMAND_NAME)
            .then(CommandManager.argument("filename", StringArgumentType.string())
            	.executes(ctx -> SchematicTaskScheduler.enqueueAnimation(ctx.getSource(),
            			StringArgumentType.getString(ctx, "filename"),
            			FabricAdapter.adapt(BlockPos.ofFloored(ctx.getSource().getPosition())),
						null, null, null, null, null, null, null, null, STOP))
	            .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
	                .executes(ctx -> SchematicTaskScheduler.enqueueAnimation(ctx.getSource(),
	                		StringArgumentType.getString(ctx, "filename"),
	                		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                		null, null, null, null, null, null, null, null, STOP))
	                .then(CommandManager.argument("ticksPerFrame", IntegerArgumentType.integer(1))
	                    .executes(ctx -> SchematicTaskScheduler.enqueueAnimation(ctx.getSource(),
	                    		StringArgumentType.getString(ctx, "filename"),
	                    		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                            IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                            null, null, null, null, null, null, null, STOP))
	                    .then(CommandManager.argument("start", IntegerArgumentType.integer())
	                        .executes(ctx -> SchematicTaskScheduler.enqueueAnimation(ctx.getSource(),
	                        		StringArgumentType.getString(ctx, "filename"),
	                        		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                IntegerArgumentType.getInteger(ctx, "start"),
	                                null, null, null, null, null, null, STOP))
	                        .then(CommandManager.argument("end", IntegerArgumentType.integer())
	                            .executes(ctx -> SchematicTaskScheduler.enqueueAnimation(ctx.getSource(),
	                            		StringArgumentType.getString(ctx, "filename"),
	                            		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                    IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                    IntegerArgumentType.getInteger(ctx, "start"),
	                                    IntegerArgumentType.getInteger(ctx, "end"),
	                                    null, null, null, null, null, STOP))
	                            .then(CommandManager.argument("step", IntegerArgumentType.integer())
	                                .executes(ctx -> SchematicTaskScheduler.enqueueAnimation(ctx.getSource(),
	                                		StringArgumentType.getString(ctx, "filename"),
	                                		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                        IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                        IntegerArgumentType.getInteger(ctx, "start"),
	                                        IntegerArgumentType.getInteger(ctx, "end"),
	                                        IntegerArgumentType.getInteger(ctx, "step"), 
	                                        null, null, null, null, STOP))
	                                .then(CommandManager.argument("loop", BoolArgumentType.bool())
	                                    .executes(ctx -> SchematicTaskScheduler.enqueueAnimation(ctx.getSource(),
	                                    		StringArgumentType.getString(ctx, "filename"),
	                                    		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                            IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                            IntegerArgumentType.getInteger(ctx, "start"),
	                                            IntegerArgumentType.getInteger(ctx, "end"),
	                                            IntegerArgumentType.getInteger(ctx, "step"),
	                                            BoolArgumentType.getBool(ctx, "loop"), 
	                                            null, null, null, STOP))
	                                    .then(CommandManager.argument("removeWhenDone", BoolArgumentType.bool())
	                                        .executes(ctx -> SchematicTaskScheduler.enqueueAnimation(ctx.getSource(),
	                                        		StringArgumentType.getString(ctx, "filename"),
	                                        		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                                IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                                IntegerArgumentType.getInteger(ctx, "start"),
	                                                IntegerArgumentType.getInteger(ctx, "end"),
	                                                IntegerArgumentType.getInteger(ctx, "step"),
	                                                BoolArgumentType.getBool(ctx, "loop"),
	                                                BoolArgumentType.getBool(ctx, "removeWhenDone"),
	                                                null, null, STOP))
	                                        .then(CommandManager.argument("clearPrev", BoolArgumentType.bool())
	                                            .executes(ctx -> SchematicTaskScheduler.enqueueAnimation(ctx.getSource(),
	                                            		StringArgumentType.getString(ctx, "filename"),
	                                            		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                                    IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                                    IntegerArgumentType.getInteger(ctx, "start"),
	                                                    IntegerArgumentType.getInteger(ctx, "end"),
	                                                    IntegerArgumentType.getInteger(ctx, "step"),
	                                                    BoolArgumentType.getBool(ctx, "loop"),
	                                                    BoolArgumentType.getBool(ctx, "removeWhenDone"),
	                                                    BoolArgumentType.getBool(ctx, "clearPrev"),
	                                                    null, STOP))
	                                            .then(CommandManager.argument("ignoreAir", BoolArgumentType.bool())
	                                                .executes(ctx -> SchematicTaskScheduler.enqueueAnimation(ctx.getSource(),
	                                                		StringArgumentType.getString(ctx, "filename"),
	                                                		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                                        IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                                        IntegerArgumentType.getInteger(ctx, "start"),
	                                                        IntegerArgumentType.getInteger(ctx, "end"),
	                                                        IntegerArgumentType.getInteger(ctx, "step"),
	                                                        BoolArgumentType.getBool(ctx, "loop"),
	                                                        BoolArgumentType.getBool(ctx, "removeWhenDone"),
	                                                        BoolArgumentType.getBool(ctx, "clearPrev"),
	                                                        BoolArgumentType.getBool(ctx, "ignoreAir"),
	                                                        STOP))
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
}
