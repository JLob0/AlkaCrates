package com.alkacode.crates.engine;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

/** Spawner de uma entidade modelo (ModelEngine/BetterModel/CraftEngine). Null se falhar. */
public interface ModelSpawner {

    /** Spawna a entidade modelo no local. Retorna a entidade ou null em falha. */
    Entity spawn(String modelId, Location location);

    boolean isAvailable();
}
