package com.facetorched.schemplacer.schematic;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.sk89q.worldedit.extent.clipboard.Clipboard;

public class SchematicCache {
	private static final Map<String, SoftReference<Clipboard>> SCHEMATIC_CACHE = new ConcurrentHashMap<>();
	
	public static Clipboard get(String key) {
		SoftReference<Clipboard> ref = SCHEMATIC_CACHE.get(key);
		if (ref != null) {
			return ref.get();
		}
		return null;
	}
	
	public static CompletableFuture<Clipboard> getFuture(String key) {
		Clipboard clipboard = get(key);
		if (clipboard != null) {
			return CompletableFuture.completedFuture(clipboard);
		}
		return null;
	}
	
	public static void put(String key, Clipboard clipboard) {
		SCHEMATIC_CACHE.put(key, new SoftReference<>(clipboard));
	}
	
	public static void put(String key, CompletableFuture<Clipboard> future) {
		future.thenAccept(clipboard -> {
			put(key, clipboard);
		});
	}
	
	public static void clear() {
		SCHEMATIC_CACHE.clear();
	}
	
	public static boolean containsKey(String key) {
		return SCHEMATIC_CACHE.containsKey(key) && get(key) != null;
	}
	
	public static Set<String> keySet() {
		return SCHEMATIC_CACHE.keySet();
	}
	
}
