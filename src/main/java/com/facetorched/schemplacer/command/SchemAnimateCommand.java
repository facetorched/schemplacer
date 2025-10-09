package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.schematic.SchematicAnimationTask;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector3;

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
            	.executes(ctx -> enqueueHelper(ctx.getSource(),
            			StringArgumentType.getString(ctx, "filename"),
            			FabricAdapter.adapt(BlockPos.ofFloored(ctx.getSource().getPosition())),
						null, null, null, null, null, null, null, null))
	            .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
	                .executes(ctx -> enqueueHelper(ctx.getSource(),
	                		StringArgumentType.getString(ctx, "filename"),
	                		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                		null, null, null, null, null, null, null, null))
	                .then(CommandManager.argument("ticksPerFrame", IntegerArgumentType.integer())
	                    .executes(ctx -> enqueueHelper(ctx.getSource(),
	                    		StringArgumentType.getString(ctx, "filename"),
	                    		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                            IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                            null, null, null, null, null, null, null))
	                    .then(CommandManager.argument("start", IntegerArgumentType.integer())
	                        .executes(ctx -> enqueueHelper(ctx.getSource(),
	                        		StringArgumentType.getString(ctx, "filename"),
	                        		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                IntegerArgumentType.getInteger(ctx, "start"),
	                                null, null, null, null, null, null))
	                        .then(CommandManager.argument("end", IntegerArgumentType.integer())
	                            .executes(ctx -> enqueueHelper(ctx.getSource(),
	                            		StringArgumentType.getString(ctx, "filename"),
	                            		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                    IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                    IntegerArgumentType.getInteger(ctx, "start"),
	                                    IntegerArgumentType.getInteger(ctx, "end"),
	                                    null, null, null, null, null))
	                            .then(CommandManager.argument("step", IntegerArgumentType.integer())
	                                .executes(ctx -> enqueueHelper(ctx.getSource(),
	                                		StringArgumentType.getString(ctx, "filename"),
	                                		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                        IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                        IntegerArgumentType.getInteger(ctx, "start"),
	                                        IntegerArgumentType.getInteger(ctx, "end"),
	                                        IntegerArgumentType.getInteger(ctx, "step"), 
	                                        null, null, null, null))
	                                .then(CommandManager.argument("loop", BoolArgumentType.bool())
	                                    .executes(ctx -> enqueueHelper(ctx.getSource(),
	                                    		StringArgumentType.getString(ctx, "filename"),
	                                    		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                            IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                            IntegerArgumentType.getInteger(ctx, "start"),
	                                            IntegerArgumentType.getInteger(ctx, "end"),
	                                            IntegerArgumentType.getInteger(ctx, "step"),
	                                            BoolArgumentType.getBool(ctx, "loop"), 
	                                            null, null, null))
	                                    .then(CommandManager.argument("removeWhenDone", BoolArgumentType.bool())
	                                        .executes(ctx -> enqueueHelper(ctx.getSource(),
	                                        		StringArgumentType.getString(ctx, "filename"),
	                                        		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                                IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                                IntegerArgumentType.getInteger(ctx, "start"),
	                                                IntegerArgumentType.getInteger(ctx, "end"),
	                                                IntegerArgumentType.getInteger(ctx, "step"),
	                                                BoolArgumentType.getBool(ctx, "loop"),
	                                                BoolArgumentType.getBool(ctx, "removeWhenDone"),
	                                                null, null))
	                                        .then(CommandManager.argument("clearPrev", BoolArgumentType.bool())
	                                            .executes(ctx -> enqueueHelper(ctx.getSource(),
	                                            		StringArgumentType.getString(ctx, "filename"),
	                                            		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                                    IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                                    IntegerArgumentType.getInteger(ctx, "start"),
	                                                    IntegerArgumentType.getInteger(ctx, "end"),
	                                                    IntegerArgumentType.getInteger(ctx, "step"),
	                                                    BoolArgumentType.getBool(ctx, "loop"),
	                                                    BoolArgumentType.getBool(ctx, "removeWhenDone"),
	                                                    BoolArgumentType.getBool(ctx, "clearPrev"),
	                                                    null))
	                                            .then(CommandManager.argument("ignoreAir", BoolArgumentType.bool())
	                                                .executes(ctx -> enqueueHelper(ctx.getSource(),
	                                                		StringArgumentType.getString(ctx, "filename"),
	                                                		FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
	                                                        IntegerArgumentType.getInteger(ctx, "ticksPerFrame"),
	                                                        IntegerArgumentType.getInteger(ctx, "start"),
	                                                        IntegerArgumentType.getInteger(ctx, "end"),
	                                                        IntegerArgumentType.getInteger(ctx, "step"),
	                                                        BoolArgumentType.getBool(ctx, "loop"),
	                                                        BoolArgumentType.getBool(ctx, "removeWhenDone"),
	                                                        BoolArgumentType.getBool(ctx, "clearPrev"),
	                                                        BoolArgumentType.getBool(ctx, "ignoreAir")))
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
    
    private static int enqueueHelper(ServerCommandSource source, String filenamePattern, BlockVector3 pastePos, Integer ticksPerFrame, Integer start, Integer end, Integer step, Boolean loop, Boolean removeWhenDone, Boolean clearPrevFrame, Boolean ignoreAir) {
    	SchematicAnimationTask task;
    	try { 
    		task = new SchematicAnimationTask(source, filenamePattern, pastePos, ticksPerFrame, start, end, step, loop, removeWhenDone, clearPrevFrame, ignoreAir);
    	} catch (Exception e) { 
			return 0; 
		}
    	return task.enqueue(STOP) ? 1 : 0;
	}
}
