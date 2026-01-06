package com.facetorched.schemplacer.schematic;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SchematicTaskDescription {
	private final String name;
	private String[] keys;
	private Object[] values;
	public SchematicTaskDescription(String name, String[] keys, Object[] values) {
		this.name = name;
		assert(keys.length == values.length);
		this.keys = keys;
		this.values = values;
	}
	
	public Text getTextDescription() {
		MutableText t = Text.empty();
		t.append(Text.literal(name).formatted(Formatting.AQUA));
		if (keys.length == 0) {
			return t;
		}
		t.append(Text.literal(" ["));
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			Object value = values[i];
			t.append(Text.literal(key).formatted(Formatting.GOLD));
			t.append(Text.literal("=").formatted(Formatting.WHITE));
			if (value instanceof SchematicTaskDescription)
				t.append(((SchematicTaskDescription)value).getTextDescription());
			else if (value instanceof ISchematicTask)
				t.append(((ISchematicTask)value).getDescription().getTextDescription());
			else
				t.append(Text.literal(value.toString()).formatted(Formatting.GREEN));
			if (i < keys.length - 1) {
				t.append(Text.literal(", "));
			}
		}
		t.append(Text.literal("]"));
		return t;
	}
	
	// TODO: read and write from JSON?
}
