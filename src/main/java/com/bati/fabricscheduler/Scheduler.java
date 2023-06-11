package com.bati.fabricscheduler;

import com.bati.fabricscheduler.impl.FabricSchedulerImpl;
import com.bati.fabricscheduler.model.FabricScheduler;
import com.bati.fabricscheduler.model.FabricWorker;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scheduler implements ModInitializer {
    public static final Logger LOGGER = Logger.getLogger("fabric-scheduler");

	private static FabricSchedulerImpl scheduler;
	@Override
	public void onInitialize() {
		scheduler = new FabricSchedulerImpl();


		ServerLifecycleEvents.SERVER_STOPPING.register(server -> onDisable());
		ServerTickEvents.END_SERVER_TICK.register(server -> scheduler.mainThreadHeartbeat(server.getTicks()));
	}

	public static FabricScheduler get() {
		return scheduler;
	}

	public void reload() {
		int pollCount = 0;

		// Wait for at most 2.5 seconds for plugins to close their threads
		while (pollCount < 50 && get().getActiveWorkers().size() > 0) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
			pollCount++;
		}

		List<FabricWorker> overdueWorkers = get().getActiveWorkers();
		for (FabricWorker worker : overdueWorkers) {
			Mod plugin = worker.getOwner();
			String author = "<NoAuthorGiven>";
			if (plugin.getAuthors().size() > 0) {
				author = plugin.getAuthors().get(0).getName();
			}
			LOGGER.log(Level.SEVERE, String.format(
					"Nag author: '%s' of '%s' about the following: %s",
					author,
					plugin.getId(),
					"This plugin is not properly shutting down its async tasks when it is being reloaded.  This may cause conflicts with the newly loaded version of the plugin"
			));
		}
	}

	public static void onDisable() {
		get().cancelAllTasks();
	}
	public void onDisablePlugin(Mod mod) {
		get().cancelTasks(mod);
	}
}