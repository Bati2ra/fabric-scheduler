package com.bati.fabricscheduler;

import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.schedule.Schedules;

import java.time.Duration;

public record SchedulerHooks(Scheduler scheduler) {

    /**
     * Synchronous tasks running in the main thread are processed as asynchronous tasks which, once executed,
     * adds the task to be executed to the queue of the main thread.
     */
    public void scheduleTask(String id, Runnable runnable) {
        scheduleTask(id, runnable, Duration.ZERO);
    }

    /**
     * Synchronous tasks running in the main thread are processed as asynchronous tasks which, once executed,
     * adds the task to be executed to the queue of the main thread.
     */
    public void scheduleTask(String id, Runnable runnable, Duration duration) {
        scheduleAsyncTask(id, () -> FabricScheduler.queue.add(runnable), duration);
    }

    /**
     * Synchronous tasks running in the main thread are processed as asynchronous tasks which, once executed,
     * adds the task to be executed to the queue of the main thread.
     */
    public void scheduleTask(String id, Runnable runnable, Duration delay, Duration interval) {
        scheduleAsyncTask(id, () -> FabricScheduler.queue.add(runnable), delay, interval);
    }

    /**
     * Synchronous tasks running in the main thread are processed as asynchronous tasks which, once executed,
     * adds the task to be executed to the queue of the main thread.
     */
    public void scheduleTask(String id, Runnable runnable, String time) {
        scheduleAsyncTask(id, () -> FabricScheduler.queue.add(runnable), time);
    }

    /**
     * Synchronous tasks running in the main thread are processed as asynchronous tasks which, once executed,
     * adds the task to be executed to the queue of the main thread.
     */
    public void scheduleAsyncTask(String id, Runnable runnable) {
        scheduleAsyncTask(id, runnable, Duration.ZERO);
    }

    /**
     * Runs a task only once, with an initial delay
     */
    public void scheduleAsyncTask(String id, Runnable runnable, Duration duration) {
        scheduler.schedule(id, runnable, Schedules.executeOnce(Schedules.fixedDelaySchedule(duration)));
    }

    /**
     * Run a task for the first time with an initial delay, the following times will be based on a given interval.
     */
    public void scheduleAsyncTask(String id, Runnable runnable, Duration delay, Duration interval) {
        scheduler.schedule(id, runnable, Schedules.afterInitialDelay(Schedules.fixedDelaySchedule(interval), delay));
    }

    /**
     * Run a task only once at a specified hour.
     *
     * @param time a specified time, format: "hh:mm", e.g.: "05:30".
     */
    public void scheduleAsyncTask(String id, Runnable runnable, String time) {
        scheduler.schedule(id, runnable, Schedules.executeOnce(Schedules.executeAt(time)));
    }
}
