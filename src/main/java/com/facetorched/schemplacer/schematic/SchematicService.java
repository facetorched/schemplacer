package com.facetorched.schemplacer.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.util.CommandBlockUtil;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.fabric.FabricFakePlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.io.file.FilenameException;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SchematicService {

    /** Common entry point used by commands and item events. */
    public static int enqueue(ServerCommandSource source, String filename, BlockVector3 pastePos, boolean remove, boolean ignoreAir) {
    	try {
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
            	return 0;
            }
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error queuing schematic: " + e.getMessage()));
            return 0;
        }
    }

    /** Loads a Clipboard using WorldEdit's safe path checks and format detection. */
    public static CompletableFuture<Clipboard> loadClipboardSafe(ServerCommandSource source, String filename) throws Exception {
        WorldEdit we = WorldEdit.getInstance();
        LocalConfiguration cfg = we.getConfiguration();
        String ext = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getPrimaryFileExtension(); // "schem"
        if (!filename.endsWith("." + ext)) filename = filename + "." + ext;
        File dir = we.getWorkingDirectoryPath(cfg.saveDir).toFile();
        Actor actor;
        if (source.getPlayer() != null)
			actor = FabricAdapter.adaptPlayer(source.getPlayer());
		else 
			actor = null;
        if (actor == null) {
        	actor = FabricAdapter.adaptPlayer(new FabricFakePlayer(source.getWorld()));
        }
        File file;
        try {
            file = we.getSafeOpenFile(actor, dir, filename, ext, ClipboardFormats.getFileExtensionArray());
        } catch (FilenameException fe) {
            throw new IllegalArgumentException("Invalid path: " + fe.getMessage());
        }
        if (!file.exists()) throw new IllegalArgumentException("File not found: " + file.getAbsolutePath());
        
        return CompletableFuture.supplyAsync(() -> {
			try {
				ClipboardFormat fmt = ClipboardFormats.findByFile(file);
		        if (fmt == null) throw new IllegalArgumentException("Unsupported format for: " + file.getName());
		        try (ClipboardReader reader = fmt.getReader(new FileInputStream(file))) {
		        	return reader.read();
		        }
			} catch (Exception e) {
				throw new RuntimeException("Error reading schematic: " + e.getMessage(), e);
			}
		});
    }
}