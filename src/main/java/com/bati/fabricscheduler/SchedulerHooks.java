package com.bati.fabricscheduler;

import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.schedule.Schedules;

import java.time.Duration;

public record SchedulerHooks(Scheduler scheduler) {

    /**
     * Synchronous tasks running in the main thread are processed as asynchronous tasks which, once executed,
     * adds the task to be executed to the queue of the main thread.
     */
    public void scheduleTask(Runnable runnable) {
        scheduleTask(runnable, Duration.ZERO);
    }

    /**
     * Synchronous tasks running in the main thread are processed as asynchronous tasks which, once executed,
     * adds the task to be executed to the queue of the main thread.
     */
    public void scheduleTask(Runnable runnable, Duration duration) {
        scheduleAsyncTask(() -> FabricScheduler.queue.add(runnable), duration);
    }

    /**
     * Synchronous tasks running in the main thread are processed as asynchronous tasks which, once executed,
     * adds the task to be executed to the queue of the main thread.
     */
    public void scheduleTask(Runnable runnable, Duration delay, Duration interval) {
        scheduleAsyncTask(() -> FabricScheduler.queue.add(runnable), delay, interval);
    }

    /**
     * Synchronous tasks running in the main thread are processed as asynchronous tasks which, once executed,
     * adds the task to be executed to the queue of the main thread.
     */
    public void scheduleTask(Runnable runnable, String time) {
        scheduleAsyncTask(() -> FabricScheduler.queue.add(runnable), time);
    }

    /**
     * Synchronous tasks running in the main thread are processed as asynchronous tasks which, once executed,
     * adds the task to be executed to the queue of the main thread.
     */
    public void scheduleAsyncTask(Runnable runnable) {
        scheduleAsyncTask(runnable, Duration.ZERO);
    }

    /**
     * Runs a task only once, with an initial delay
     */
    public void scheduleAsyncTask(Runnable runnable, Duration duration) {
        scheduler.schedule(runnable, Schedules.executeOnce(Schedules.fixedDelaySchedule(duration)));
    }

    /**
     * Run a task for the first time with an initial delay, the following times will be based on a given interval.
     */
    public void scheduleAsyncTask(Runnable runnable, Duration delay, Duration interval) {
        scheduler.schedule(runnable, Schedules.afterInitialDelay(Schedules.fixedDelaySchedule(interval), delay));
    }

    /**
     * Run a task only once at a specified hour.
     *
     * @param time a specified time, format: "hh:mm", e.g.: "05:30".
     */
    public void scheduleAsyncTask(Runnable runnable, String time) {
        scheduler.schedule(runnable, Schedules.executeOnce(Schedules.executeAt(time)));
    }
}
