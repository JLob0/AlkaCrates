package com.alkacode.crates.engine;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;

/** Spawner de modelos da ModelEngine (Limesharp) via reflection. */
public final class ModelEngineSpawner implements ModelSpawner {

    private static boolean failed = false;
    private static Object api;
    private static Method spawnMethod;

    public ModelEngineSpawner() {
        try {
            Class<?> modelEngineClass = Class.forName("com.ticxo.modelengine.api.ModelEngine");
            api = modelEngineClass.getMethod("getAPI").invoke(null);
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
            if (entity instanceof Entity e) {
                return e;
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }
}
