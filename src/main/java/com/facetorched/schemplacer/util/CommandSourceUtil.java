package com.facetorched.schemplacer.util;

import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;

public class CommandSourceUtil {
	/**
     * Produce a stable identity key for the source.
     */
	public static Object getSourceKey(ServerCommandSource source) {
        CommandBlockBlockEntity cbbe = CommandBlockUtil.getCommandBlockEntityFromSource(source);
        if (cbbe != null) {
            return cbbe.getPos(); // BlockPos is stable & equals/hashCode safe
        }
        Entity entity = source.getEntity();
        if (entity != null) {
            return entity.getUuid(); // UUID is stable across ticks
        }
        return null;
    }
}
