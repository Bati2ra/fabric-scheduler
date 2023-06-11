package com.bati.fabricscheduler.model;

import com.bati.fabricscheduler.Mod;

/**
 * Represents a worker thread for the scheduler. This gives information about
 * the Thread object for the task, owner of the task and the taskId.
 * <p>
 * Workers are used to execute async tasks.
 */
public interface FabricWorker {

    /**
     * Returns the taskId for the task being executed by this worker.
     *
     * @return Task id number
     */
    public int getTaskId();

    /**
     * Returns the Mod that owns this task.
     *
     * @return The Mod that owns the task
     */
    public Mod getOwner();

    /**
     * Returns the thread for the worker.
     *
     * @return The Thread object for the worker
     */
    public Thread getThread();

}