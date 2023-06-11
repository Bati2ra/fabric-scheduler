package com.bati.fabricscheduler.model;

import com.bati.fabricscheduler.Mod;

/**
 * Represents a task being executed by the scheduler
 */
public interface FabricTask {

    /**
     * Returns the taskId for the task.
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
     * Returns true if the Task is a sync task.
     *
     * @return true if the task is run by main thread
     */
    public boolean isSync();

    /**
     * Will attempt to cancel this task.
     */
    public void cancel();
}