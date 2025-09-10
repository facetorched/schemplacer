package com.facetorched.schemplacer.util;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Helpers for working with command blocks from a ServerCommandSource. */
public final class CommandBlockUtil {
    private CommandBlockUtil() {}
    
    public static CommandBlockBlockEntity getCommandBlockEntityFromSource(ServerCommandSource source) {
    	if (source == null) return null;
		ServerWorld world = source.getWorld();
		BlockPos pos = BlockPos.ofFloored(source.getPosition());
		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof CommandBlockBlockEntity cbb) {
			return cbb;
		}
		return null;
	}

    /** Returns true if the command source appears to be a command block at its source position. */
    public static boolean isCommandBlockSource(ServerCommandSource source) {
        return getCommandBlockEntityFromSource(source) != null;
    }

    /** Set the command block success count and update comparators (if source is a command block). */
    public static void setCommandBlockSuccess(ServerCommandSource source, int success) {
        CommandBlockBlockEntity cbb = getCommandBlockEntityFromSource(source);
        if (cbb != null) {
            // Update success count
            cbb.getCommandExecutor().setSuccessCount(success);
            cbb.markDirty();

            // Push comparator updates
            ServerWorld world = source.getWorld();
            BlockPos pos = BlockPos.ofFloored(source.getPosition());
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            world.updateComparators(pos, block);
        }
    }
    
    public static int getCommandBlockSuccess(ServerCommandSource source) {
		CommandBlockBlockEntity cbb = getCommandBlockEntityFromSource(source);
		if (cbb != null) {
			return cbb.getCommandExecutor().getSuccessCount();
		}
		return 0;
	}
}
