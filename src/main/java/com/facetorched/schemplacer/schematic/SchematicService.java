package com.facetorched.schemplacer.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.util.SchemNotFoundException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.io.file.FilenameException;
import com.sk89q.worldedit.world.block.BlockTypes;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SchematicService {
	public static final Map<String, CompletableFuture<Clipboard>> SCHEMATIC_CACHE = new HashMap<String, CompletableFuture<Clipboard>>();
    
    /** Load a schematic file, using cache if enabled. */
    public static CompletableFuture<Clipboard> loadClipboardSafe(ServerCommandSource source, String filename) {
    	if (SchemPlacerMod.CONFIG.cacheSchematics) {
    		if (SCHEMATIC_CACHE.containsKey(filename)) {
    			return SCHEMATIC_CACHE.get(filename);
    		}
    	}
    	File file;
    	try {
    		file = getSchematicFile(filename); // errors if not found
    	}
    	catch (SchemNotFoundException e) {
			CompletableFuture<Clipboard> failedFuture = new CompletableFuture<>();
			failedFuture.completeExceptionally(e);
			return failedFuture; // completed and not added to cache
		}
		CompletableFuture<Clipboard> future = CompletableFuture.supplyAsync(() -> {
			ClipboardFormat fmt = ClipboardFormats.findByFile(file);
	        if (fmt == null) throw new IllegalArgumentException("Unsupported format for: " + file.getName());
	        try (ClipboardReader reader = fmt.getReader(new FileInputStream(file))) {
	        	return reader.read();
	        } catch (IOException ioe) {
	        	throw new RuntimeException("Error reading schematic: " + ioe.getMessage(), ioe);
	        }
		});
		if (SchemPlacerMod.CONFIG.cacheSchematics) {
			SCHEMATIC_CACHE.put(filename, future);
    	}
    	return future;
    }
    
    public static File getSchematicFile(String filename) {
    	File dir = SchemPlacerMod.CONFIG.getSchematicDir();
        Actor actor = null; // no actor needed if opening file.
        File file;
        try {
        	file = WorldEdit.getInstance().getSafeOpenFile(actor, dir, filename, 
        			BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getPrimaryFileExtension(), 
        			ClipboardFormats.getFileExtensionArray());
        } catch (FilenameException fe) {
            throw new IllegalArgumentException("Invalid path: " + fe.getMessage());
        }
        if (!file.exists()) throw new SchemNotFoundException("Schematic not found: " + file.getAbsolutePath());
        return file;
    }
    
    public static boolean schematicExists(String filename) {
		try {
			getSchematicFile(filename);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
    
    /** Create an efficient difference clipboard between two schematics. */
    public static Clipboard diffClipboard(
            ServerCommandSource source,
            Clipboard oldClipboard,
            Clipboard newClipboard,
            boolean ignoreAir) {

    	if (!ignoreAir) return newClipboard;
    	if (oldClipboard == null) {
    		source.sendError(Text.literal("Cannot diff with previous frame: previous frame is null."));
    	}
    	Clipboard diffClipboard = new BlockArrayClipboard(newClipboard.getRegion());
    	diffClipboard.setOrigin(newClipboard.getOrigin());
        int minX = Math.min(oldClipboard.getMinimumPoint().x(), newClipboard.getMinimumPoint().x());
        int minY = Math.min(oldClipboard.getMinimumPoint().y(), newClipboard.getMinimumPoint().y());
        int minZ = Math.min(oldClipboard.getMinimumPoint().z(), newClipboard.getMinimumPoint().z());
        int maxX = Math.max(oldClipboard.getMaximumPoint().x(), newClipboard.getMaximumPoint().x());
        int maxY = Math.max(oldClipboard.getMaximumPoint().y(), newClipboard.getMaximumPoint().y());
        int maxZ = Math.max(oldClipboard.getMaximumPoint().z(), newClipboard.getMaximumPoint().z());
        
        for (int x = minX; x <= maxX; x++) {
        	for (int y = minY; y <= maxY; y++) {
        		for (int z = minZ; z <= maxZ; z++) {
                    BlockVector3 pos = BlockVector3.at(x, y, z);
                    var oldBlock = oldClipboard.getBlock(pos);
                    var newBlock = newClipboard.getBlock(pos);
                    boolean newIsAir = newBlock.getBlockType().id().equals("minecraft:air");
                    boolean oldIsAir = oldBlock.getBlockType().id().equals("minecraft:air");
                    
                    try {
                        if (newIsAir && !oldIsAir) {
                        	diffClipboard.setBlock(pos, BlockTypes.VOID_AIR.getDefaultState());
                        } else if (!newBlock.equals(oldBlock)) {
                        	diffClipboard.setBlock(pos, newBlock);
                        } else {
                        	diffClipboard.setBlock(pos, BlockTypes.AIR.getDefaultState());
                        }
					} catch (WorldEditException e) {}
                }
            }
        }
        return diffClipboard;
    }
}