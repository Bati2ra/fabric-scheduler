package com.bati.fabricscheduler.model;

import com.bati.fabricscheduler.Mod;
import com.bati.fabricscheduler.Scheduler;

public abstract class FabricRunnable implements Runnable {
    private int taskId = -1;

    /**
     * Attempts to cancel this task.
     *
     * @throws IllegalStateException if task was not scheduled yet
     */
    public synchronized void cancel() throws IllegalStateException {
        Scheduler.get().cancelTask(getTaskId());
    }

    /**
     * Schedules this in the Fabric scheduler to run on next tick.
     *
     * @param mod the reference to the mod scheduling task
     * @return a FabricTask that contains the id number
     * @throws IllegalArgumentException if mod is null
     * @throws IllegalStateException if this was already scheduled
     * @see FabricScheduler#runTask(Mod, Runnable)
     */
    public synchronized FabricTask runTask(Mod mod) throws IllegalArgumentException, IllegalStateException {
        checkState();
        return setupId(Scheduler.get().runTask(mod, (Runnable) this));
    }

    /**
     * <b>Asynchronous tasks should never access any API in Fabric. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Schedules this in the Fabric scheduler to run asynchronously.
     *
     * @param mod the reference to the mod scheduling task
     * @return a FabricTask that contains the id number
     * @throws IllegalArgumentException if mod is null
     * @throws IllegalStateException if this was already scheduled
     * @see FabricScheduler#runTaskAsynchronously(Mod, Runnable)
     */
    public synchronized FabricTask runTaskAsynchronously(Mod mod) throws IllegalArgumentException, IllegalStateException  {
        checkState();
        return setupId(Scheduler.get().runTaskAsynchronously(mod, (Runnable) this));
    }

    /**
     * Schedules this to run after the specified number of server ticks.
     *
     * @param mod the reference to the mod scheduling task
     * @param delay the ticks to wait before running the task
     * @return a FabricTask that contains the id number
     * @throws IllegalArgumentException if mod is null
     * @throws IllegalStateException if this was already scheduled
     * @see FabricScheduler#runTaskLater(Mod, Runnable, long)
     */
    public synchronized FabricTask runTaskLater(Mod mod, long delay) throws IllegalArgumentException, IllegalStateException  {
        checkState();
        return setupId(Scheduler.get().runTaskLater(mod, (Runnable) this, delay));
    }

    /**
     * <b>Asynchronous tasks should never access any API in Fabric. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Schedules this to run asynchronously after the specified number of
     * server ticks.
     *
     * @param mod the reference to the mod scheduling task
     * @param delay the ticks to wait before running the task
     * @return a FabricTask that contains the id number
     * @throws IllegalArgumentException if mod is null
     * @throws IllegalStateException if this was already scheduled
     * @see FabricScheduler#runTaskLaterAsynchronously(Mod, Runnable, long)
     */
    public synchronized FabricTask runTaskLaterAsynchronously(Mod mod, long delay) throws IllegalArgumentException, IllegalStateException  {
        checkState();
        return setupId(Scheduler.get().runTaskLaterAsynchronously(mod, (Runnable) this, delay));
    }

    /**
     * Schedules this to repeatedly run until cancelled, starting after the
     * specified number of server ticks.
     *
     * @param mod the reference to the mod scheduling task
     * @param delay the ticks to wait before running the task
     * @param period the ticks to wait between runs
     * @return a FabricTask that contains the id number
     * @throws IllegalArgumentException if mod is null
     * @throws IllegalStateException if this was already scheduled
     * @see FabricScheduler#runTaskTimer(Mod, Runnable, long, long)
     */
    public synchronized FabricTask runTaskTimer(Mod mod, long delay, long period) throws IllegalArgumentException, IllegalStateException  {
        checkState();
        return setupId(Scheduler.get().runTaskTimer(mod, (Runnable) this, delay, period));
    }

    /**
     * <b>Asynchronous tasks should never access any API in Fabric. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Schedules this to repeatedly run asynchronously until cancelled,
     * starting after the specified number of server ticks.
     *
     * @param mod the reference to the mod scheduling task
     * @param delay the ticks to wait before running the task for the first
     *     time
     * @param period the ticks to wait between runs
     * @return a FabricTask that contains the id number
     * @throws IllegalArgumentException if mod is null
     * @throws IllegalStateException if this was already scheduled
     * @see FabricScheduler#runTaskTimerAsynchronously(Mod, Runnable, long,
     *     long)
     */
    public synchronized FabricTask runTaskTimerAsynchronously(Mod mod, long delay, long period) throws IllegalArgumentException, IllegalStateException  {
        checkState();
        return setupId(Scheduler.get().runTaskTimerAsynchronously(mod, (Runnable) this, delay, period));
    }

    /**
     * Gets the task id for this runnable.
     *
     * @return the task id that this runnable was scheduled as
     * @throws IllegalStateException if task was not scheduled yet
     */
    public synchronized int getTaskId() throws IllegalStateException {
        final int id = taskId;
        if (id == -1) {
            throw new IllegalStateException("Not scheduled yet");
        }
        return id;
    }

    private void checkState() {
        if (taskId != -1) {
            throw new IllegalStateException("Already scheduled as " + taskId);
        }
    }

    private FabricTask setupId(final FabricTask task) {
        this.taskId = task.getTaskId();
        return task;
    }

}
