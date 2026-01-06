package com.facetorched.schemplacer.schematic;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.facetorched.schemplacer.SchemPlacerMod;
import com.facetorched.schemplacer.util.CommandBlockUtil;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SchematicStreamTask implements ISchematicTask {
    private final ServerCommandSource source;
    private final int port;
    private final BlockVector3 pastePos;
    private final boolean skipLaggingFrames;
    private final boolean clearPrevFrame;
    private final boolean removeWhenDone;
    private final boolean ignoreAir;

    private boolean commandOutput = true;
    private boolean done = false;
    private boolean paused = false;
    private final Queue<CompletableFuture<Clipboard>> incomingQueue = new ConcurrentLinkedQueue<>();
    private int frameCounter = 0;
    private SchematicPlaceTask currentTask = null;
    private CompletableFuture<Clipboard> prevClipboardFuture = null;
    private String portName = null;
    
    // Networking
    private ServerSocket serverSocket;
    private Thread listenerThread;

    public SchematicStreamTask(
            ServerCommandSource source,
            int port,
            BlockVector3 pastePos,
            Boolean skipLaggingFrames,
            Boolean removeWhenDone,
            Boolean clearPrevFrame,
            Boolean ignoreAir) {

        this.source = source;
        this.port = port;
        this.pastePos = pastePos;
        this.skipLaggingFrames = skipLaggingFrames != null ? skipLaggingFrames : true;
        this.removeWhenDone = removeWhenDone != null ? removeWhenDone : true;
        this.clearPrevFrame = clearPrevFrame != null ? clearPrevFrame : true;
        this.ignoreAir = ignoreAir != null ? ignoreAir : true;
        portName = (port == 0 ? "automaticallyAllocated" : Integer.toString(port));
        this.commandOutput = SchemPlacerMod.CONFIG.commandOutput && source != null;
    }

    private void startListener() {
        try {
            this.serverSocket = new ServerSocket(port);
            this.portName = (port == 0) ? "automaticallyAllocated(" + Integer.toString(serverSocket.getLocalPort()) + ")" : Integer.toString(port);
            if (commandOutput) source.sendFeedback(() -> Text.literal("Schematic Stream listening on port " + this.portName), true);
        } catch (IOException e) {
        	if (commandOutput) source.sendError(Text.literal("Failed to bind port " + this.portName + ": " + e.getMessage()));
            this.done = true;
            return;
        }

        this.listenerThread = new Thread(() -> {
            while (!stopping()) {
                try {
                    Socket client = serverSocket.accept();
                    // client.setSoTimeout(5000); // Optional: set a timeout for reading
                    // Pass the stream to the service. We assume the client sends one schematic then closes/finishes.
                    CompletableFuture<Clipboard> future = SchematicService.loadClipboardStream(client.getInputStream());
                    // when done, close the client socket.
                    future.whenComplete((clipboard, ex) -> {
						try {
							client.close();
						} catch (IOException e) {
							// Ignore for now
						}
					});
                    incomingQueue.add(future);
                } catch (IOException e) {
                    // Socket closed or error accepting, likely due to stop() being called
                }
            }
        });
        this.listenerThread.setName("SchematicStreamListener-" + portName);
        this.listenerThread.setDaemon(true);
        this.listenerThread.start();
    }

    @Override
    public int tick(int batchSize) {
        if (done) return batchSize;
        if (CommandBlockUtil.isCommandBlockSource(source)) {
            CommandBlockUtil.setCommandBlockSuccess(source, 0);
        }
        //if (paused) return batchSize;

        // If we have a current task running, finish or tick it
        if (currentTask != null) {
            if (currentTask.clipboardErrored()) {
                Throwable ex = currentTask.getClipboardError();
                if (commandOutput) source.sendError(Text.literal("Error reading streamed schematic " + frameCounter + ": " + (ex != null ? ex.getMessage() : "unknown")));
                currentTask = null; // Discard failed task
                frameCounter++; // TODO: should we skip frame numbers on error?
            } else if (currentTask.isDone()) {
                if (!stopping()) prevClipboardFuture = currentTask.getClipboardFuture();
                currentTask = null; // Ready for next
                frameCounter++;
            } else {
                return currentTask.tick(batchSize);
            }
        }
        
        // If no current task, get a new one from the queue
        if (currentTask == null) {
        	CompletableFuture<Clipboard> nextFuture = null;
        	if (stopping()) { // Socket closed: no more incoming frames
        		if (removeWhenDone && prevClipboardFuture != null) {
    	            currentTask = new SchematicPlaceTask(
    	                source,
    	                "stream:" + port + "#cleanup", // Unique ID
    	                prevClipboardFuture, // Used as the clipboard to define the removal area
    	                pastePos,
    	                ignoreAir,
    	                true, // remove = true
    	                true, // silent
    	                null
    	            );
    	            prevClipboardFuture = null; // only remove once
    	            return currentTask.tick(batchSize);
        		}
        		else {
        			done = true;
        		}
        	}
        	else if (skipLaggingFrames) { // Skip to the latest available frame
                while (!incomingQueue.isEmpty()) {
                    if (nextFuture != null) { // We have multiple frames
                    	if (!incomingQueue.peek().isDone()) { // Next frame not ready yet
                    		break;
                    	}
                        nextFuture.cancel(false); // Cancel skipped frame to free resources
                    }
                    nextFuture = incomingQueue.poll();
                }
            } else { // Otherwise, take the next frame in order
                nextFuture = incomingQueue.poll();
            }
            if (nextFuture != null) {
                CompletableFuture<Clipboard> removeFuture = null;
                if (clearPrevFrame) {
                    removeFuture = prevClipboardFuture;
                }
                currentTask = new SchematicPlaceTask(
                    source,
                    "stream:" + port + "#" + frameCounter, // Unique ID
                    nextFuture,
                    pastePos,
                    ignoreAir,
                    false, // remove = false
                    true,  // Silent
                    removeFuture
                );
                return currentTask.tick(batchSize);
            }
        }
        return batchSize;
    }
    
    private boolean stopping() {
		return done || (serverSocket == null || serverSocket.isClosed());
	}

    @Override
    public void stop() {
        try {
            if (!stopping()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignore
        } finally {
			serverSocket = null;
		}
        if (commandOutput) source.sendFeedback(() -> Text.literal("Stopping schematic stream on port " + portName), true);
    }

    @Override
    public boolean enqueue(boolean stop) {
        if (CommandBlockUtil.isCommandBlockSource(source)) {
            CommandBlockUtil.setCommandBlockSuccess(source, 0);
        }
        
        if (stop) {
            // Check if this specific task is already running to stop it
            ISchematicTask existing = SchematicTaskQueue.findTask(this);
            if (existing != null) {
                existing.stop();
                return true;
            }
            if (commandOutput) source.sendFeedback(() -> Text.literal("No stream to stop on port " + portName), true);
            return false;
        }
        // Start the task
        boolean queueSuccess = SchematicTaskQueue.enqueue(this);
        if (!queueSuccess) {
        	if (commandOutput) source.sendError(Text.literal("Error queuing stream task (task already exists on port " + portName + ")"));
            return false;
        }
        startListener();
        return true;
    }

    @Override
    public boolean togglePause() {
        return paused = !paused;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public boolean isDone() {
        return done;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(port, pastePos, skipLaggingFrames, clearPrevFrame, removeWhenDone, ignoreAir);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SchematicStreamTask other)) return false;
        return port == other.port
            && Objects.equals(pastePos, other.pastePos)
            && skipLaggingFrames == other.skipLaggingFrames
            && clearPrevFrame == other.clearPrevFrame
            && removeWhenDone == other.removeWhenDone
            && ignoreAir == other.ignoreAir;
    }
    
    @Override
    public SchematicTaskDescription getDescription() {
		return new SchematicTaskDescription(
			"SchematicStreamTask",
			new String[] {"port", "pastePos", "skipLaggingFrames", "clearPrevFrame", "removeWhenDone", "ignoreAir"},
			new Object[] {port, pastePos, skipLaggingFrames, clearPrevFrame, removeWhenDone, ignoreAir}
		);
	}
}