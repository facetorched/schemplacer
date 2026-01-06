package com.facetorched.schemplacer.schematic;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import com.facetorched.schemplacer.SchemPlacerMod;

import net.minecraft.server.MinecraftServer;

public class SchematicTaskQueue {
	private static final LinkedHashSet<ISchematicTask> TASK_QUEUE = new LinkedHashSet<>();
	
	public static boolean isTaskQueued(ISchematicTask task) {
		synchronized (TASK_QUEUE) {
			return TASK_QUEUE.contains(task);
		}
	}

    public static boolean enqueue(ISchematicTask task) {
        synchronized (TASK_QUEUE) {
            return TASK_QUEUE.add(task);
        }
    }

    public static void onServerTick(MinecraftServer server) {
        if (TASK_QUEUE.isEmpty()) return;
        Iterator<ISchematicTask> it;
        synchronized (TASK_QUEUE) {
            it = new LinkedList<>(TASK_QUEUE).iterator();
        }
        int batchSize = SchemPlacerMod.CONFIG.batchSize;
        while (it.hasNext()) {
        	ISchematicTask task = it.next();
            batchSize = task.tick(batchSize);
            if (task.isDone()) {
                synchronized (TASK_QUEUE) {
                    TASK_QUEUE.remove(task);
                }
            }
        }
    }
    
    public static ISchematicTask findTask(ISchematicTask task) {
		synchronized (TASK_QUEUE) {
			for (ISchematicTask t : TASK_QUEUE) {
				if (t.equals(task)) return t;
			}
		}
		return null;
	}
    
    public static ISchematicTask[] getQueuedTasks() {
    	synchronized (TASK_QUEUE) {
    		return TASK_QUEUE.toArray(new ISchematicTask[0]);
    	}
    }
    
    public static void clearQueuedTasks() {
		synchronized (TASK_QUEUE) {
			TASK_QUEUE.clear();
		}
	}
    
}
