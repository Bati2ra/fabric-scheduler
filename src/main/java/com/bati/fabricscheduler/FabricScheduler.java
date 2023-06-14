package com.bati.fabricscheduler;

import com.coreoz.wisp.JobStatus;
import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.schedule.Schedules;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class FabricScheduler implements ModInitializer {
    // Asynchronous scheduler
    private static Scheduler scheduler;
    private static SchedulerHooks schedulerHooks;
    private static int QUEUE_CHECKING_TICKS = 2 * 20;

    public static final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    public static final Logger LOGGER = Logger.getLogger("fabric-scheduler");


    @Override
    public void onInitialize() {

        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerTickEvents.START_SERVER_TICK.register(this::processQueue);
    }

    public static SchedulerHooks getSchedulerHooks() {
        return schedulerHooks;
    }

    public static Scheduler getScheduler() {
        return scheduler;
    }

    private void registerCleanUpTask() {
        scheduler.schedule(
                "Jobs Cleaner",
                () -> scheduler
                        .jobStatus()
                        .stream()
                        .filter(job -> job.status() == JobStatus.DONE)
                        // Clean only jobs that have finished executing since at least 10 seconds
                        .filter(job -> job.lastExecutionEndedTimeInMillis() < (System.currentTimeMillis() - 10000))
                        .forEach(job -> scheduler.remove(job.name())),
                Schedules.fixedDelaySchedule(Duration.ofMinutes(10))
        );
    }

    private void processQueue(MinecraftServer server) {
        if(queue.isEmpty()) return;
        var ticks = server.getTicks();

        if(ticks % QUEUE_CHECKING_TICKS == 0) queue.remove().run();
    }

    private void onServerStopping(MinecraftServer server) {
        scheduler.gracefullyShutdown();
    }

    private void onServerStarting(MinecraftServer server) {
        scheduler = new Scheduler();

        registerCleanUpTask();
        schedulerHooks = new SchedulerHooks(scheduler);

        /*
        schedulerHooks.scheduleAsyncTask(() -> System.out.println("Iniciando el servidor de forma asincrona"));
        schedulerHooks.scheduleTask(() -> System.out.println("Iniciando el servidor en el main thread!"));
        schedulerHooks.scheduleAsyncTask(() -> System.out.println("Ejecutando con delay de 10s, cada 5s de forma asincrona"), Duration.ofSeconds(10), Duration.ofSeconds(5));
        schedulerHooks.scheduleTask(() -> System.out.println("Ejecutando con delay de 10s, cada 5s en el main thread!"), Duration.ofSeconds(10), Duration.ofSeconds(5));
        */
    }
}
