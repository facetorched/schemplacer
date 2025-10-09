package com.facetorched.schemplacer.schematic;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.util.eventbus.Subscribe;

public class SchematicCacheLoader {
	public static final Logger LOGGER = LoggerFactory.getLogger(SchemPlacerMod.MOD_ID);

    @Subscribe
    public void onConfigLoad(PlatformReadyEvent event) {
    	reloadCache(false);
    }
    
    public static boolean reloadCache(boolean clearExisting) {
    	if (!SchemPlacerMod.CONFIG.cacheSchematics) return false;
    	if (clearExisting) SchematicService.SCHEMATIC_CACHE.clear();
    	File dir = SchemPlacerMod.CONFIG.getSchematicDir();
		LOGGER.info("Schematic caching is enabled. Loading schematics from " + dir.getAbsolutePath());
		if (dir.exists() && dir.isDirectory()) {
			File[] files = dir.listFiles((d, name) -> name.endsWith(".schem") || name.endsWith(".schematic"));
			if (files != null) {
				for (File f : files) {
					int dotIdx = f.getName().lastIndexOf('.');
					if (dotIdx <= 0) continue;
					String name = f.getName().substring(0, dotIdx);
					if (SchematicService.SCHEMATIC_CACHE.containsKey(name)) {
						LOGGER.warn("Duplicate schematic name found: " + name);
						continue;
					}
					CompletableFuture<Clipboard> future = SchematicService.loadClipboardSafe(null, name);
					SchematicService.SCHEMATIC_CACHE.put(name, future);
					future.whenComplete((cb, ex) -> {
						if (ex != null) {
							LOGGER.warn("Failed to load schematic " + name, ex);
						} else {
							LOGGER.info("Cached schematic: " + name);
						}
					});
				}
			}
			return true;
		} else {
			LOGGER.info("Schematic cache directory does not exist: " + dir.getAbsolutePath());
		}
		return false;
	}
}
