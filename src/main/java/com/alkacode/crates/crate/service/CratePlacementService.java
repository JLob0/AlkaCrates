package com.alkacode.crates.crate.service;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.CrateLocation;
import com.alkacode.crates.crate.placement.PlacedCrate;
import com.alkacode.crates.display.CrateDisplay;
import com.alkacode.crates.engine.CrateEngine;
import com.alkacode.crates.engine.CrateEngineType;
import com.alkacode.crates.engine.ModelSpawner;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

/** Coloca/remove crates no mundo e carrega as persistidas do banco. */
public final class CratePlacementService {

    private final AlkaCrates plugin;
    private final CrateEngine engine;
    private final Map<CrateEngineType, ModelSpawner> modelSpawners = new EnumMap<>(CrateEngineType.class);

    public CratePlacementService(AlkaCrates plugin, CrateEngine engine) {
        this.plugin = plugin;
        this.engine = engine;
    }

    public void registerModelSpawner(CrateEngineType type, ModelSpawner spawner) {
        modelSpawners.put(type, spawner);
    }

    public boolean placeAt(Crate crate, Location location, boolean persist) {
        CrateDisplay display = engine.createDisplay(crate, location);
        if (display == null || !display.isValid()) {
            return false;
        }
        // engine de modelo: spawna a entidade extra se disponivel
        ModelSpawner spawner = modelSpawners.get(crate.getEngineType());
        if (spawner != null && spawner.isAvailable()) {
            Entity model = spawner.spawn(crate.getVanillaItem(), location);
            if (model != null) {
                display.setModelEntity(model);
            }
        }
        PlacedCrate placed = new PlacedCrate(crate, location, display);
        plugin.getPlacedCrateManager().register(placed);
        plugin.getAnimationEngine().startIdle(display);
        if (persist) {
            try {
                plugin.getCrateLocationRepository().save(CrateLocation.fromLocation(crate.getId(), location));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Falha ao persistir crate " + crate.getId(), e);
            }
        }
        return true;
    }

    public boolean removeAt(Location location) {
        PlacedCrate placed = plugin.getPlacedCrateManager().getAt(location);
        if (placed == null) {
            return false;
        }
        plugin.getAnimationEngine().stopIdle(placed.getDisplay());
        placed.remove();
        plugin.getPlacedCrateManager().unregister(location);
        try {
            plugin.getCrateLocationRepository().delete(location);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao remover crate do banco", e);
        }
        return true;
    }

    /** Carrega todas as crates persistidas do banco. */
    public void loadAll() {
        try {
            for (CrateLocation cl : plugin.getCrateLocationRepository().findAll()) {
                Crate crate = plugin.getCratesConfig().getCrate(cl.getCrateId());
                if (crate == null) {
                    continue;
                }
                Location location = cl.toLocation();
                if (location != null) {
                    placeAt(crate, location, false);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao carregar crates persistidas", e);
        }
    }
}
