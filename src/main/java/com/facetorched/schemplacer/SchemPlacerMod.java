package com.facetorched.schemplacer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facetorched.schemplacer.command.SchemAnimateCommand;
import com.facetorched.schemplacer.command.SchemItemCommand;
import com.facetorched.schemplacer.command.SchemPlaceCommand;
import com.facetorched.schemplacer.command.SchemReloadCacheCommand;
import com.facetorched.schemplacer.command.SchemSequenceCommand;
import com.facetorched.schemplacer.command.SchemStreamCommand;
import com.facetorched.schemplacer.command.SchemTaskCommand;
import com.facetorched.schemplacer.command.SchemWaitCommand;
import com.facetorched.schemplacer.event.RightClickHandler;
import com.facetorched.schemplacer.schematic.SchematicCacheLoader;
import com.facetorched.schemplacer.schematic.SchematicTaskQueue;
import com.facetorched.schemplacer.util.ModConfig;
import com.sk89q.worldedit.WorldEdit;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class SchemPlacerMod implements ModInitializer {
    public static final String MOD_ID = "schemplacer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ModConfig CONFIG;

    @Override
    public void onInitialize() {
        // Load config
    	CONFIG = ModConfig.loadOrCreate();
        // Register commands
        CommandRegistrationCallback.EVENT.register(SchemPlaceCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemItemCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemAnimateCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemSequenceCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemWaitCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemReloadCacheCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemStreamCommand::register);
        CommandRegistrationCallback.EVENT.register(SchemTaskCommand::register);
        // Register right-click handler
        RightClickHandler.register();
        // Tick runner
        ServerTickEvents.END_SERVER_TICK.register(SchematicTaskQueue::onServerTick);
        
        if (CONFIG.cacheSchematics) {
			WorldEdit.getInstance().getEventBus().register(new SchematicCacheLoader());
		}
        LOGGER.info("Schematic Placer initialized.");
    }
    
    
}