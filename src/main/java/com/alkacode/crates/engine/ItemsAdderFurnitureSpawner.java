package com.alkacode.crates.engine;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;

/**
 * Spawner de furniture (BlockBench) do ItemsAdder via reflection - best-effort, mesmo
 * padrao de ModelEngineSpawner/BetterModelSpawner/CraftEngineSpawner. Assinatura real
 * NAO confirmada contra o jar (sem acesso a servidor live pra javap) - se
 * `CustomFurniture#spawn(String, Location)` nao bater com a versao instalada, ajustar
 * aqui (so este arquivo precisa mudar).
 */
public final class ItemsAdderFurnitureSpawner implements ModelSpawner {

    private static boolean failed = false;
    private static Method spawnMethod;

    public ItemsAdderFurnitureSpawner() {
        try {
            Class<?> customFurnitureClass = Class.forName("dev.lone.itemsadder.api.CustomFurniture");
            spawnMethod = customFurnitureClass.getMethod("spawn", String.class, Location.class);
        } catch (Throwable t) {
            failed = true;
        }
    }

    public static boolean checkAvailable() {
        return !failed && spawnMethod != null;
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
            Object result = spawnMethod.invoke(null, modelId, location);
            if (result instanceof Entity e) {
                return e;
            }
            // algumas versoes retornam um wrapper (ex: FurnitureBaseEntity) em vez da Entity direto.
            if (result != null) {
                try {
                    Object entity = result.getClass().getMethod("getEntity").invoke(result);
                    if (entity instanceof Entity e) {
                        return e;
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }
}
