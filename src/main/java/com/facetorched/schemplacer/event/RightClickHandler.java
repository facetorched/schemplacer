package com.facetorched.schemplacer.event;

import com.facetorched.schemplacer.schematic.SchematicService;
import com.facetorched.schemplacer.util.LoreParser;
import com.sk89q.worldedit.math.BlockVector3;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public class RightClickHandler {

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            ItemStack held = sp.getStackInHand(hand);
            boolean invert = sp.isSneaking();
            LoreParser.Parsed parsed = LoreParser.parse(held);
            if (parsed == null) return ActionResult.PASS;
            BlockVector3 pos;
            if (parsed.x == null || parsed.y == null || parsed.z == null) {
                // get player position if not specified in lore
        		pos = BlockVector3.at(
					(int)Math.floor(sp.getX()),
					(int)Math.floor(sp.getY()),
					(int)Math.floor(sp.getZ())
				);
            }
            else {
				pos = BlockVector3.at(parsed.x, parsed.y, parsed.z);
			}
            boolean remove = parsed.remove;
            if (invert) remove = !remove;
            String filename = parsed.filename;
            int res = SchematicService.enqueue(sp.getCommandSource(), filename, pos, remove, true);
            if (res > 0) {
            	player.swingHand(hand, true);
                return ActionResult.SUCCESS;
            }
            return ActionResult.FAIL;
        });
    }
}