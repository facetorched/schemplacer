package com.facetorched.schemplacer;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facetorched.schemplacer.command.SchemPlaceCommand;
import com.facetorched.schemplacer.command.SchemReloadCacheCommand;
import com.facetorched.schemplacer.command.SchemRemoveCommand;
import com.facetorched.schemplacer.command.SchemSequenceCommand;
import com.facetorched.schemplacer.command.SchemStopAnimateCommand;
import com.facetorched.schemplacer.command.SchemStopSequenceCommand;
import com.facetorched.schemplacer.command.SchemStopWaitCommand;
import com.facetorched.schemplacer.command.SchemWaitCommand;
import com.facetorched.schemplacer.command.SchemAnimateCommand;
import com.facetorched.schemplacer.command.SchemItemCommand;
import com.facetorched.schemplacer.event.RightClickHandler;
import com.facetorched.schemplacer.schematic.ISchematicTask;
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
    private static final LinkedHashSet<ISchematicTask> TASK_QUEUE = new LinkedHashSet<>();

    @Override
    public void onInitialize() {
        // Load config
    	CONFIG = ModConfig.loadOrCreate();
        // Register commands
        CommandRegistrationCallback.EVENT.register(SchemPlaceCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemRemoveCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemItemCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemAnimateCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemStopAnimateCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemSequenceCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemStopSequenceCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemWaitCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemStopWaitCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemReloadCacheCommand::register);
        // Register right-click handler
        RightClickHandler.register();
        // Tick runner
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        
        if (CONFIG.cacheSchematics) {
			WorldEdit.getInstance().getEventBus().register(new SchematicCacheLoader());
		}
        LOGGER.info("Schematic Placer initialized.");
    }
    
    public static boolean isTaskQueued(ISchematicTask task) {
		synchronized (TASK_QUEUE) {
			return TASK_QUEUE.contains(task);
		}
	}

    public static boolean enqueue(ISchematicTask task) {
        synchronized (TASK_QUEUE) {
            return TASK_QUEUE.add(task);
        }
    }

    private void onServerTick(MinecraftServer server) {
        if (TASK_QUEUE.isEmpty()) return;
        Iterator<ISchematicTask> it;
        synchronized (TASK_QUEUE) {
            it = new LinkedList<>(TASK_QUEUE).iterator();
        }
        int batchSize = CONFIG.batchSize;
        while (it.hasNext()) {
        	ISchematicTask task = it.next();
            batchSize = task.tick(batchSize);
            if (task.isDone()) {
                synchronized (TASK_QUEUE) {
                    TASK_QUEUE.remove(task);
                }
            }
        }
    }
    
    public static ISchematicTask findTask(ISchematicTask task) {
		synchronized (TASK_QUEUE) {
			for (ISchematicTask t : TASK_QUEUE) {
				if (t.equals(task)) return t;
			}
		}
		return null;
	}
}