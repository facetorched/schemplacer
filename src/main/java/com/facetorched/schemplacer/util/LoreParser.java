package com.facetorched.schemplacer.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;

import java.util.Locale;

import com.facetorched.schemplacer.command.PlaceSchemCommand;
import com.facetorched.schemplacer.command.RemoveSchemCommand;

public class LoreParser {

    public static class Parsed {
        public final boolean remove;
        public final String filename;
        public final Integer x, y, z; // optional (can be null)
        public Parsed(boolean remove, String filename, Integer x, Integer y, Integer z) {
            this.remove = remove;
            this.filename = filename;
            this.x = x; this.y = y; this.z = z;
        }
    }

    /**
     * Parse first lore line of an item and run commands.
     */
    public static Parsed parse(ItemStack stack) {
        LoreComponent lore = stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT);
        if (lore == null || lore.lines().isEmpty()) return null;
        String line = lore.lines().get(0).getString().trim();
        if (line.isEmpty()) return null;
        String[] toks = line.split("\\s+");
        if (toks.length < 2) return null;
        String cmd = toks[0].toLowerCase(Locale.ROOT);
        boolean remove = false;
        if (cmd.equals(PlaceSchemCommand.COMMAND_NAME)) {}
		else if (cmd.equals(RemoveSchemCommand.COMMAND_NAME)) {
			remove = true;
		} else {
			return null;
		}
        String filename = toks[1];
        Integer x=null,y=null,z=null;
        if (toks.length >= 5) {
            try {
                x = Integer.parseInt(toks[2]);
                y = Integer.parseInt(toks[3]);
                z = Integer.parseInt(toks[4]);
            } catch (NumberFormatException ignored) {}
        }
        return new Parsed(remove, filename, x, y, z);
    }
}