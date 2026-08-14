package com.alkacode.crates.engine;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;

/** Spawner de modelos do BetterModel via reflection. */
public final class BetterModelSpawner implements ModelSpawner {

    private static boolean failed = false;
    private static Object api;
    private static Method spawnMethod;

    public BetterModelSpawner() {
        try {
            Class<?> betterModelClass = Class.forName("dev.kalkafox.bettermodel.api.BetterModelAPI");
            api = betterModelClass.getMethod("getAPI").invoke(null);
            // melhor esforco: spawnModel(String, Location) ou (String, String, Location)
            try {
                spawnMethod = api.getClass().getMethod("spawnModel", String.class, Location.class);
            } catch (NoSuchMethodException e) {
                spawnMethod = api.getClass().getMethod("spawnModel", String.class, String.class, Location.class);
            }
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
            Object entity = spawnMethod.getParameterCount() == 3
                    ? spawnMethod.invoke(api, modelId, "default", location)
                    : spawnMethod.invoke(api, modelId, location);
            return entity instanceof Entity e ? e : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
