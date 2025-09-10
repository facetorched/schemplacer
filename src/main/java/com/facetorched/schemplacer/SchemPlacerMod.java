package com.facetorched.schemplacer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Path;
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
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public class SchemPlacerMod implements ModInitializer {
    public static final String MOD_ID = "schemplacer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static int BATCH_SIZE = 10000; // blocks per tick

    private static final Set<SchematicBatchTask> TASK_QUEUE = new HashSet<>();

    @Override
    public void onInitialize() {
        loadConfig();
        // Register commands
        CommandRegistrationCallback.EVENT.register(PlaceSchemCommand::register);
        CommandRegistrationCallback.EVENT.register(RemoveSchemCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemItemCommand::register);
        // Register right-click handler
        RightClickHandler.register();
        // Tick runner
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        LOGGER.info("Schematic Placer initialized. batchSize={}", BATCH_SIZE);
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
        int batchSize = BATCH_SIZE;
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

    private void loadConfig() {
        try {
            Path cfgDir = FabricLoader.getInstance().getConfigDir();
            File cfg = cfgDir.resolve(MOD_ID + ".json").toFile();
            if (!cfg.exists()) {
                JsonObject o = new JsonObject();
                o.addProperty("batchSize", BATCH_SIZE);
                try (FileWriter w = new FileWriter(cfg)) {
                    new Gson().toJson(o, w);
                }
                return;
            }
            try (Reader r = new FileReader(cfg)) {
                JsonObject o = new Gson().fromJson(r, JsonObject.class);
                if (o != null && o.has("batchSize")) {
                    BATCH_SIZE = Math.max(0, o.get("batchSize").getAsInt());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load config; using defaults", e);
        }
    }
}