package com.bati.fabricscheduler.impl;

import com.bati.fabricscheduler.Mod;
import com.bati.fabricscheduler.model.FabricWorker;
import org.apache.commons.lang.UnhandledException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

class FabricAsyncTask extends FabricTaskImpl {

    private final LinkedList<FabricWorker> workers = new LinkedList<FabricWorker>();
    private final Map<Integer, FabricTaskImpl> runners;

    FabricAsyncTask(final Map<Integer, FabricTaskImpl> runners, final Mod mod, final Runnable task, final int id, final long delay) {
        super(mod, task, id, delay);
        this.runners = runners;
    }

    @Override
    public boolean isSync() {
        return false;
    }

    @Override
    public void run() {
        final Thread thread = Thread.currentThread();
        synchronized(workers) {
            if (getPeriod() == -2) {
                // Never continue running after cancelled.
                // Checking this with the lock is important!
                return;
            }
            workers.add(
                    new FabricWorker() {
                        public Thread getThread() {
                            return thread;
                        }

                        public int getTaskId() {
                            return FabricAsyncTask.this.getTaskId();
                        }

                        public Mod getOwner() {
                            return FabricAsyncTask.this.getOwner();
                        }
                    });
        }
        Throwable thrown = null;
        try {
            super.run();
        } catch (final Throwable t) {
            thrown = t;
            throw new UnhandledException(
                    String.format(
                            "Mod %s generated an exception while executing task %s",
                            getOwner().getName(),
                            getTaskId()),
                    thrown);
        } finally {
            // Cleanup is important for any async task, otherwise ghost tasks are everywhere
            synchronized(workers) {
                try {
                    final Iterator<FabricWorker> workers = this.workers.iterator();
                    boolean removed = false;
                    while (workers.hasNext()) {
                        if (workers.next().getThread() == thread) {
                            workers.remove();
                            removed = true; // Don't throw exception
                            break;
                        }
                    }
                    if (!removed) {
                        throw new IllegalStateException(
                                String.format(
                                        "Unable to remove worker %s on task %s for %s",
                                        thread.getName(),
                                        getTaskId(),
                                        getOwner().getName()),
                                thrown); // We don't want to lose the original exception, if any
                    }
                } finally {
                    if (getPeriod() < 0 && workers.isEmpty()) {
                        // At this spot, we know we are the final async task being executed!
                        // Because we have the lock, nothing else is running or will run because delay < 0
                        runners.remove(getTaskId());
                    }
                }
            }
        }
    }

    LinkedList<FabricWorker> getWorkers() {
        return workers;
    }

    boolean cancel0() {
        synchronized (workers) {
            // Synchronizing here prevents race condition for a completing task
            setPeriod(-2l);
            if (workers.isEmpty()) {
                runners.remove(getTaskId());
            }
        }
        return true;
    }
}