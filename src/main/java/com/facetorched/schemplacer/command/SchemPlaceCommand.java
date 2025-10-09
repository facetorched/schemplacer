package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.schematic.SchematicPlaceTask;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector3;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;

public class SchemPlaceCommand {
	public static final String COMMAND_NAME = "schemplace";
	public static final boolean REMOVE_DEFAULT = false;
	
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
        dispatcher.register(CommandManager.literal(COMMAND_NAME)
            .then(CommandManager.argument("filename", StringArgumentType.string())
        		.executes(ctx -> enqueueHelper(ctx.getSource(), 
                		StringArgumentType.getString(ctx, "filename"), 
                		FabricAdapter.adapt(BlockPos.ofFloored(ctx.getSource().getPosition())), 
                		null))
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                    .executes(ctx -> enqueueHelper(ctx.getSource(), 
							StringArgumentType.getString(ctx, "filename"), 
							FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")), 
							null))
                    .then(CommandManager.argument("ignoreAir", BoolArgumentType.bool())
						.executes(ctx -> enqueueHelper(ctx.getSource(), 
								StringArgumentType.getString(ctx, "filename"),
						        FabricAdapter.adapt(BlockPosArgumentType.getLoadedBlockPos(ctx, "pos")),
						        BoolArgumentType.getBool(ctx, "ignoreAir")))
					)
                )
            )
        );
    }
    
    private static int enqueueHelper(ServerCommandSource source, String filename, BlockVector3 pastePos, Boolean ignoreAir) {
    	SchematicPlaceTask task;
    	try { 
    		task = new SchematicPlaceTask(source, filename, pastePos, ignoreAir, REMOVE_DEFAULT);
    	} catch (Exception e) { 
			return 0; 
		}
    	return task.enqueue(false) ? 1 : 0;
	}
}