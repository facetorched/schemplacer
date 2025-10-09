package com.facetorched.schemplacer.event;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.schematic.SchematicSequenceTask;
import com.facetorched.schemplacer.util.UnknownSchemCommandException;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class RightClickHandler {

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            ItemStack stack = sp.getStackInHand(hand);
            LoreComponent lore = stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT);
            if (lore == null || lore.lines().isEmpty()) return ActionResult.PASS;
            String [] taskList = lore.lines().stream().map(t -> t.getString().trim()).toArray(String[]::new);
            SchematicSequenceTask task;
            try {
            	 task = new SchematicSequenceTask(sp.getCommandSource(), taskList);
            } catch (Exception e) {
            	if (e instanceof UnknownSchemCommandException) {
					// don't spam unknown command errors
					return ActionResult.PASS;
				}
				if (SchemPlacerMod.CONFIG.commandOutput) {
					String msg = e != null ? e.getMessage() : "Failed to parse command sequence";
					sp.sendMessage(Text.literal(msg), false);
				}
				return ActionResult.FAIL;
			}
            boolean stop = player.isSneaking();
            if (task != null && task.enqueue(stop)) {
            	player.swingHand(hand, true);
                return ActionResult.SUCCESS;
			}
            return ActionResult.FAIL;
        });
    }
}