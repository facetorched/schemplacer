package com.facetorched.schemplacer.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // === Defaults ===
    public int batchSize = 10_000;
    public boolean cacheSchematics = false;
    public String schematicDir = "config/worldedit/schematics";

    // === File ===
    private static final String FILE_NAME = SchemPlacerMod.MOD_ID + ".json";

    public static ModConfig loadOrCreate() {
        Path cfgDir = FabricLoader.getInstance().getConfigDir();
        File cfgFile = cfgDir.resolve(FILE_NAME).toFile();

        ModConfig cfg = new ModConfig(); // start with defaults

        try {
            if (cfgFile.exists()) {
                try (Reader r = new FileReader(cfgFile)) {
                    JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
                    merge(json, cfg); // update fields from file
                }
            }
        } catch (Exception e) {
            System.err.println("[SchemPlacer] Failed to read config, using defaults: " + e);
        }
        // Always re-save
        cfg.save(cfgFile);
        return cfg;
    }

    private static void merge(JsonObject json, ModConfig cfg) {
        if (json.has("batchSize")) {
            cfg.batchSize = Math.max(0, json.get("batchSize").getAsInt());
        }
        if (json.has("cacheSchematics")) {
            cfg.cacheSchematics = json.get("cacheSchematics").getAsBoolean();
        }
        if (json.has("schematicDir")) {
            cfg.schematicDir = json.get("schematicDir").getAsString();
        }
    }
    
    private void save(File file) {
        try (Writer w = new FileWriter(file)) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            System.err.println("[SchemPlacer] Failed to write config: " + e);
        }
    }

    public File getSchematicDir() {
        File dir = new File(schematicDir);
        if (!dir.isAbsolute()) {
            dir = new File(FabricLoader.getInstance().getGameDir().toFile(), schematicDir);
        }
        return dir;
    }
}
