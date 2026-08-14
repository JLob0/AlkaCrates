package com.alkacode.crates.crate.placement;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Registro de todas as crates colocadas no mundo, por localizacao. */
public final class PlacedCrateManager {

    private final Map<Location, PlacedCrate> placed = new ConcurrentHashMap<>();

    public void register(PlacedCrate crate) {
        placed.put(crate.getLocation(), crate);
    }

    public void unregister(Location location) {
        placed.remove(location);
    }

    public PlacedCrate getAt(Location location) {
        return placed.get(location);
    }

    public List<PlacedCrate> getAll() {
        return new ArrayList<>(placed.values());
    }

    public void removeAll() {
        for (PlacedCrate crate : new ArrayList<>(placed.values())) {
            crate.remove();
        }
        placed.clear();
    }
}
