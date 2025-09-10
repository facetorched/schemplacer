package com.facetorched.schemplacer.command;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class SchemItemCommand {
	public static final String COMMAND_NAME = "schemitem";
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
        dispatcher.register(CommandManager.literal(COMMAND_NAME)
            .requires(src -> src.hasPermissionLevel(0)) // players can run
            .then(CommandManager.argument("line", StringArgumentType.greedyString())
                .executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    ServerPlayerEntity player = source.getPlayer(); // throws if not a player; fine for our use case
                    if (player == null) {
                        source.sendError(Text.literal("This command can only be used by a player."));
                        return 0;
                    }

                    ItemStack stack = player.getMainHandStack();
                    if (stack.isEmpty()) {
                        source.sendError(Text.literal("Hold the item in your main hand to add lore."));
                        return 0;
                    }

                    String line = StringArgumentType.getString(ctx, "line").trim();
                    if (line.isEmpty()) {
                        source.sendError(Text.literal("Lore line cannot be empty."));
                        return 0;
                    }

                    // Make it non-italic, white
                    MutableText newLine = Text.literal(line).formatted(Formatting.WHITE).styled(s -> s.withItalic(false));
                    List<Text> newLines = new ArrayList<>();
                    newLines.add(newLine);
                    
                    // Read existing lore (may be null) and append all existing lines after the new one
                    LoreComponent existing = stack.get(DataComponentTypes.LORE);
                    if (existing != null && existing.lines() != null && !existing.lines().isEmpty()) {
                        newLines.addAll(existing.lines());
                    }

                    stack.set(DataComponentTypes.LORE, new LoreComponent(newLines));
                    source.sendFeedback(() ->
                        Text.literal("Added lore to item: ").append(Text.literal(line).formatted(Formatting.AQUA)),
                        false
                    );
                    return 1;
                })
            )
        );
    }
}
