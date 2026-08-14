package com.alkacode.crates.engine;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;

/** Spawner de modelos do CraftEngine via reflection. */
public final class CraftEngineSpawner implements ModelSpawner {

    private static boolean failed = false;
    private static Object api;
    private static Method spawnMethod;

    public CraftEngineSpawner() {
        try {
            Class<?> craftEngineClass = Class.forName("com.gp.craftengine.api.CraftEngineAPI");
            api = craftEngineClass.getMethod("getAPI").invoke(null);
            spawnMethod = api.getClass().getMethod("spawnModel", String.class, Location.class);
        } catch (Throwable t) {
            failed = true;
        }
    }

    public static boolean checkAvailable() {
        return !failed && api != null;
    }

    @Override
    public boolean isAvailable() {
        return checkAvailable();
    }

    @Override
    public Entity spawn(String modelId, Location location) {
        if (!isAvailable()) {
            return null;
        }
        try {
            Object entity = spawnMethod.invoke(api, modelId, location);
            return entity instanceof Entity e ? e : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
