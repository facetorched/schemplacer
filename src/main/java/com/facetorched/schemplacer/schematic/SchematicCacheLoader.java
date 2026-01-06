package com.facetorched.schemplacer.schematic;

import java.io.File;
import java.util.HashSet;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.util.eventbus.Subscribe;

public class SchematicCacheLoader {
    @Subscribe
    public void onConfigLoad(PlatformReadyEvent event) {
    	reloadCache(false);
    }
    
    public static boolean reloadCache(boolean clearExisting) {
    	if (!SchemPlacerMod.CONFIG.cacheSchematics) return false;
    	if (clearExisting) SchematicCache.clear();
    	File dir = SchemPlacerMod.CONFIG.getSchematicDir();
		SchemPlacerMod.LOGGER.info("Schematic caching is enabled. Loading schematics from " + dir.getAbsolutePath());
		HashSet<String> cachedStems = new HashSet<>(SchematicCache.keySet());
		if (dir.exists() && dir.isDirectory()) {
			File[] files = dir.listFiles((d, name) -> name.endsWith(".schem") || name.endsWith(".schematic"));
			for (File f : files) {
				String name = f.getName();
				String filestem = SchematicService.getFilestem(name);
				if (cachedStems.contains(filestem)) {
					SchemPlacerMod.LOGGER.warn("Duplicate schematic name found: " + filestem + ". Skipping caching of " + name);
					continue;
				}
				cachedStems.add(filestem);
				SchematicService.loadClipboardSafe(null, name);
			}
			return true;
		} else {
			SchemPlacerMod.LOGGER.info("Cannot cache schematics. Directory does not exist: " + dir.getAbsolutePath());
		}
		return false;
	}
}
