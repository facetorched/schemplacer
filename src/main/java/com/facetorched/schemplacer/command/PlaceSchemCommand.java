package com.facetorched.schemplacer.command;

import com.facetorched.schemplacer.schematic.SchematicService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import com.sk89q.worldedit.math.BlockVector3;

public class PlaceSchemCommand {
	public static final String COMMAND_NAME = "placeschem";
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
        dispatcher.register(CommandManager.literal(COMMAND_NAME)
            .then(CommandManager.argument("filename", StringArgumentType.string())
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                    .executes(ctx -> {
                        String filename = StringArgumentType.getString(ctx, "filename");
                        BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
                        return SchematicService.enqueue(ctx.getSource(), filename, BlockVector3.at(pos.getX(), pos.getY(), pos.getZ()), false, true);
                    })
                ).executes(ctx -> {
					String filename = StringArgumentType.getString(ctx, "filename");
					// Source position if no position specified
					BlockPos pos = BlockPos.ofFloored(ctx.getSource().getPosition());
					return SchematicService.enqueue(ctx.getSource(), filename, BlockVector3.at(pos.getX(), pos.getY(), pos.getZ()), false, true);
				})
            )
        );
    }
}