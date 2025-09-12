package com.facetorched.schemplacer.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.util.CommandBlockUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.io.file.FilenameException;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SchematicService {
	public static final Map<String, CompletableFuture<Clipboard>> SCHEMATIC_CACHE = new HashMap<String, CompletableFuture<Clipboard>>();

    /** Common entry point used by commands and item events. */
    public static int enqueue(ServerCommandSource source, String filename, BlockVector3 pastePos, boolean remove, boolean ignoreAir) {
		if (CommandBlockUtil.isCommandBlockSource(source)) {
        	CommandBlockUtil.setCommandBlockSuccess(source, 0);
        }
        SchematicBatchTask task = new SchematicBatchTask(source, filename, pastePos, remove, ignoreAir);
        boolean queueSuccess = SchemPlacerMod.enqueue(task);
        if (!queueSuccess) {
			source.sendFeedback(() -> Text.literal("Already " + (remove ? "removing " : "placing ") + filename), true);
			return 0;
		}
        source.sendFeedback(() -> Text.literal((remove ? "Removing " : "Placing ") + filename), true);
        if (CommandBlockUtil.isCommandBlockSource(source)) {
        	return 0; // TODO test if this is necessary
        }
        return 1;
    }
    
    /** Load a schematic file, using cache if enabled. */
    public static CompletableFuture<Clipboard> loadClipboardSafe(ServerCommandSource source, String filename) {
    	if (SchemPlacerMod.CONFIG.cacheSchematics) {
    		if (SCHEMATIC_CACHE.containsKey(filename)) {
    			return SCHEMATIC_CACHE.get(filename);
			} else {
				CompletableFuture<Clipboard> future = loadClipboardInternal(source, filename);
				SCHEMATIC_CACHE.put(filename, future);
				return future;
    		}
    	}
    	return loadClipboardInternal(source, filename);
    }
    
    /** Internal method to load a schematic file asynchronously. */
    private static CompletableFuture<Clipboard> loadClipboardInternal(ServerCommandSource source, String filename) {
        return CompletableFuture.supplyAsync(() -> {
            File dir = SchemPlacerMod.CONFIG.getSchematicDir();
            Actor actor = null;
            if (source != null && source.getPlayer() != null)
    			actor = FabricAdapter.adaptPlayer(source.getPlayer());
            File file;
            try {
            	file = WorldEdit.getInstance().getSafeOpenFile(actor, dir, filename, 
            			BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getPrimaryFileExtension(), 
            			ClipboardFormats.getFileExtensionArray());
            } catch (FilenameException fe) {
                throw new IllegalArgumentException("Invalid path: " + fe.getMessage());
            }
            if (!file.exists()) throw new IllegalArgumentException("File not found: " + file.getAbsolutePath());
			ClipboardFormat fmt = ClipboardFormats.findByFile(file);
	        if (fmt == null) throw new IllegalArgumentException("Unsupported format for: " + file.getName());
	        try (ClipboardReader reader = fmt.getReader(new FileInputStream(file))) {
	        	return reader.read();
	        } catch (IOException ioe) {
	        	throw new RuntimeException("Error loading schematic: " + ioe.getMessage(), ioe);
	        }
		});
    }
}