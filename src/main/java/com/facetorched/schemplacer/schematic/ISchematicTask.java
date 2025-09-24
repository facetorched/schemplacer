package com.facetorched.schemplacer.schematic;

public interface ISchematicTask {
		/** Called periodically to perform work. Return true if the task is complete. */
	public int tick(int batchSize);
		/** Called to pause or unpause the task. */
	public boolean togglePause();
		/** Called to check if the task is paused. */
	public boolean isPaused();
		/** Called to pause the task. */
	public void stop();
		/** Called to check if the task is complete. */
	public boolean isDone();
}
