package com.bati.fabricscheduler.impl;


import com.bati.fabricscheduler.Mod;
import com.bati.fabricscheduler.Scheduler;
import com.bati.fabricscheduler.model.FabricTask;

class FabricTaskImpl implements FabricTask, Runnable {

    private volatile FabricTaskImpl next = null;
    /**
     * -1 means no repeating <br>
     * -2 means cancel <br>
     * -3 means processing for Future <br>
     * -4 means done for Future <br>
     * Never 0 <br>
     * >0 means number of ticks to wait between each execution
     */
    private volatile long period;
    private long nextRun;
    private final Runnable task;
    private final Mod mod;
    private final int id;

    FabricTaskImpl() {
        this(null, null, -1, -1);
    }

    FabricTaskImpl(final Runnable task) {
        this(null, task, -1, -1);
    }

    FabricTaskImpl(final Mod mod, final Runnable task, final int id, final long period) {
        this.mod = mod;
        this.task = task;
        this.id = id;
        this.period = period;
    }

    public final int getTaskId() {
        return id;
    }

    public final Mod getOwner() {
        return mod;
    }

    public boolean isSync() {
        return true;
    }

    public void run() {
        task.run();
    }

    long getPeriod() {
        return period;
    }

    void setPeriod(long period) {
        this.period = period;
    }

    long getNextRun() {
        return nextRun;
    }

    void setNextRun(long nextRun) {
        this.nextRun = nextRun;
    }

    FabricTaskImpl getNext() {
        return next;
    }

    void setNext(FabricTaskImpl next) {
        this.next = next;
    }

    Class<? extends Runnable> getTaskClass() {
        return task.getClass();
    }

    public void cancel() {
        Scheduler.get().cancelTask(id);
    }

    /**
     * This method properly sets the status to cancelled, synchronizing when required.
     *
     * @return false if it is a craft future task that has already begun execution, true otherwise
     */
    boolean cancel0() {
        setPeriod(-2l);
        return true;
    }
}