package com.facetorched.schemplacer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facetorched.schemplacer.command.PlaceSchemCommand;
import com.facetorched.schemplacer.command.RemoveSchemCommand;
import com.facetorched.schemplacer.command.SchemItemCommand;
import com.facetorched.schemplacer.event.RightClickHandler;
import com.facetorched.schemplacer.schematic.SchematicBatchTask;
import com.facetorched.schemplacer.schematic.SchematicCacheLoader;
import com.facetorched.schemplacer.util.ModConfig;
import com.sk89q.worldedit.WorldEdit;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class SchemPlacerMod implements ModInitializer {
    public static final String MOD_ID = "schemplacer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ModConfig CONFIG;
    private static final Set<SchematicBatchTask> TASK_QUEUE = new HashSet<>();

    @Override
    public void onInitialize() {
        // Load config
    	CONFIG = ModConfig.loadOrCreate();
        // Register commands
        CommandRegistrationCallback.EVENT.register(PlaceSchemCommand::register);
        CommandRegistrationCallback.EVENT.register(RemoveSchemCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemItemCommand::register);
        // Register right-click handler
        RightClickHandler.register();
        // Tick runner
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        
        if (CONFIG.cacheSchematics) {
			WorldEdit.getInstance().getEventBus().register(new SchematicCacheLoader());
		}
        LOGGER.info("Schematic Placer initialized.");
    }

    public static boolean enqueue(SchematicBatchTask task) {
        synchronized (TASK_QUEUE) {
            return TASK_QUEUE.add(task);
        }
    }

    private void onServerTick(MinecraftServer server) {
        if (TASK_QUEUE.isEmpty()) return;
        Iterator<SchematicBatchTask> it;
        synchronized (TASK_QUEUE) {
            it = new LinkedList<>(TASK_QUEUE).iterator();
        }
        int batchSize = CONFIG.batchSize;
        while (it.hasNext()) {
            SchematicBatchTask task = it.next();
            batchSize = task.tick(batchSize);
            if (task.isDone()) {
                synchronized (TASK_QUEUE) {
                    TASK_QUEUE.remove(task);
                }
            }
        }
    }
    
    
}