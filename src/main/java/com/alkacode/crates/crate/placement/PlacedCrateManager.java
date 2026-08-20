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

    /** Busca por tag (AC-###) - poucas crates colocadas em geral, scan linear e suficiente. */
    public PlacedCrate getByTag(String tag) {
        for (PlacedCrate crate : placed.values()) {
            if (tag.equalsIgnoreCase(crate.getTag())) {
                return crate;
            }
        }
        return null;
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
