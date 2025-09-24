package com.facetorched.schemplacer.event;

import java.util.Locale;

import com.facetorched.schemplacer.command.SchemAnimateCommand;
import com.facetorched.schemplacer.command.SchemPlaceCommand;
import com.facetorched.schemplacer.command.SchemRemoveCommand;
import com.facetorched.schemplacer.command.SchemStopAnimateCommand;
import com.facetorched.schemplacer.schematic.SchematicTaskScheduler;
import com.sk89q.worldedit.math.BlockVector3;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public class RightClickHandler {

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            ItemStack stack = sp.getStackInHand(hand);
            LoreComponent lore = stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT);
            if (lore == null || lore.lines().isEmpty()) return ActionResult.PASS;
            String line = lore.lines().get(0).getString().trim(); // only use the first line of lore
            if (line.isEmpty()) return ActionResult.PASS;
            String[] toks = line.split("\\s+");
            if (toks.length < 1) return ActionResult.PASS;
            String cmd = toks[0].toLowerCase(Locale.ROOT);
            
            int res = 0;
            if (cmd.equals(SchemPlaceCommand.COMMAND_NAME)) {
            	res = parseAndEnqueuePlace(sp, false, toks);
            } else if (cmd.equals(SchemRemoveCommand.COMMAND_NAME)) {
    			res = parseAndEnqueuePlace(sp, true, toks);
            } else if (cmd.equals(SchemAnimateCommand.COMMAND_NAME)) {
            	res = parseAndEnqueueAnimation(sp, false, toks);
            } else if (cmd.equals(SchemStopAnimateCommand.COMMAND_NAME)) {
            	res = parseAndEnqueueAnimation(sp, true, toks);
    		} else {
    			return ActionResult.PASS;
    		}
            if (res > 0) {
            	player.swingHand(hand, true);
                return ActionResult.SUCCESS;
            }
            return ActionResult.FAIL;
        });
    }
    
	private static int parseAndEnqueuePlace(ServerPlayerEntity sp, boolean remove, String [] toks) {
    	String filename = getFilenameArg(toks, 1);
    	if (filename == null) return 0;
        BlockVector3 pos = getPastePositionArg(sp, toks, 2);
        if (pos == null) return 0;
        if (sp.isSneaking()) remove = !remove;
        Boolean ignoreAir = getBooleanArg(toks, 5);
        if (ignoreAir == null) ignoreAir = true;
        return SchematicTaskScheduler.enqueuePlace(sp.getCommandSource(), filename, pos, ignoreAir, remove);
	}
	
    private static int parseAndEnqueueAnimation(ServerPlayerEntity sp, boolean stop, String[] toks) {
		String filenamePattern = getFilenameArg(toks, 1);
		if (filenamePattern == null) return 0;
		BlockVector3 pos = getPastePositionArg(sp, toks, 2);
		if (pos == null) return 0;
		Integer ticksPerFrame = getIntegerArg(toks, 5);
		Integer start = getIntegerArg(toks, 6);
		Integer end = getIntegerArg(toks, 7);
		Integer step = getIntegerArg(toks, 8);
		Boolean loop = getBooleanArg(toks, 9);
		Boolean removeWhenDone = getBooleanArg(toks, 10);
		Boolean clearPrevFrame = getBooleanArg(toks, 11);
		Boolean ignoreAir = getBooleanArg(toks, 12);
		if (sp.isSneaking()) stop = !stop;
		return SchematicTaskScheduler.enqueueAnimation(sp.getCommandSource(), filenamePattern, pos,
				ticksPerFrame, start, end, step, loop,
				removeWhenDone, clearPrevFrame, ignoreAir, stop);
	}
    
    private static String getFilenameArg(String[] toks, int index) {
    	if (index < toks.length) {
    		String input = toks[index];
	        if (input == null || input.length() < 2) return input;
	        char first = input.charAt(0);
	        char last = input.charAt(input.length() - 1);
	        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
	            return input.substring(1, input.length() - 1);
	        }
	        return input;
    	}
    	return null;
    }
    
    private static BlockVector3 getPastePositionArg(ServerPlayerEntity sp, String[] toks, int index) {
    	BlockVector3 pos = null;
        if (index + 2 < toks.length) { // position specified in lore
            try {
            	pos = BlockVector3.at(
            			Integer.parseInt(toks[index]),
            			Integer.parseInt(toks[index + 1]),
            			Integer.parseInt(toks[index + 2])
            	);
            } catch (NumberFormatException e) {
            	return null;
            }
        } else { // get player position if not specified in lore
			pos = BlockVector3.at(
				(int)Math.floor(sp.getX()),
				(int)Math.floor(sp.getY()),
				(int)Math.floor(sp.getZ())
			);
        }
        return pos;
    }
    
    private static Integer getIntegerArg(String[] toks, int index) {
		if (index < toks.length) {
			try {
				return Integer.parseInt(toks[index]);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
    
    private static Boolean getBooleanArg(String[] toks, int index) {
		if (index < toks.length) {
			String boolStr = toks[index].toLowerCase(Locale.ROOT);
			if (boolStr.equals("true")) return true;
			else if (boolStr.equals("false")) return false;
			else return null;
		}
		return null;
    }
}