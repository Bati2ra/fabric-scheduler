package com.bati.fabricscheduler;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;

import java.util.List;
import java.util.logging.Logger;

public class Mod {
    private boolean enabled;
    private final String id;
    private final String name;
    private final String version;
    private final Logger logger;
    private final List<Person> authors;

    public Mod(String id) {
        var container = FabricLoader.getInstance().getModContainer(id).orElseThrow();
        var metaData = container.getMetadata();

        this.id = metaData.getId();
        name = metaData.getName();
        version = metaData.getVersion().getFriendlyString();
        logger = Logger.getLogger(id);
        authors = metaData.getAuthors().stream().toList();
    }

    public List<Person> getAuthors() {
        return authors;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
