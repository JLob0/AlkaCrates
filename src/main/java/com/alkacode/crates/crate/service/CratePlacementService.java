package com.alkacode.crates.crate.service;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.CrateLocation;
import com.alkacode.crates.crate.placement.PlacedCrate;
import com.alkacode.crates.display.CrateDisplay;
import com.alkacode.crates.engine.CrateEngine;
import com.alkacode.crates.engine.CrateEngineType;
import com.alkacode.crates.engine.PhysicalChestEngine;
import com.alkacode.crates.engine.VanillaEngine;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

/** Coloca/remove crates no mundo e carrega as persistidas do banco. Cada crate colocada
 * ganha uma tag unica (AC-###, ver CrateLocationRepository#nextTag) - permite remover
 * pelo /alkacrates remove <crate>:<tag> sem precisar estar perto/mirando nela. */
public final class CratePlacementService {

    private final AlkaCrates plugin;
    private final CrateEngine engine;
    private final Map<CrateEngineType, CrateEngine> engines = new EnumMap<>(CrateEngineType.class);

    public CratePlacementService(AlkaCrates plugin) {
        this.plugin = plugin;
        this.engine = new VanillaEngine(plugin);
        engines.put(CrateEngineType.VANILLA, engine);
        engines.put(CrateEngineType.PHYSICAL_CHEST, new PhysicalChestEngine(plugin));
    }

    /** Registra um engine extra (ModelEngine/BetterModel/CraftEngine/ItemsAdder) - so quando o plugin terceiro esta presente. */
    public void registerEngine(CrateEngineType type, CrateEngine engine) {
        engines.put(type, engine);
    }

    private CrateEngine getEngine(CrateEngineType type) {
        CrateEngine crateEngine = engines.get(type);
        return crateEngine != null ? crateEngine : engine;
    }

    /** Coloca uma crate NOVA (admin) - gera uma tag unica e persiste. Retorna a tag gerada, ou null se falhou. */
    public String placeAt(Crate crate, Location location) {
        String tag;
        try {
            tag = plugin.getCrateLocationRepository().nextTag();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao gerar tag pra nova crate " + crate.getId(), e);
            return null;
        }
        if (!spawn(crate, location, tag)) {
            return null;
        }
        try {
            plugin.getCrateLocationRepository().save(CrateLocation.fromLocation(crate.getId(), location, tag));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao persistir crate " + crate.getId(), e);
        }
        return tag;
    }

    /** Recoloca uma crate ja persistida (reload/boot) - usa a tag ja salva, nao gera nem persiste de novo. */
    private boolean placeExisting(Crate crate, Location location, String tag) {
        return spawn(crate, location, tag);
    }

    private boolean spawn(Crate crate, Location location, String tag) {
        CrateDisplay display = getEngine(crate.getEngineType()).createDisplay(crate, location);
        if (display == null || !display.isValid()) {
            return false;
        }
        PlacedCrate placed = new PlacedCrate(crate, location, display, tag);
        plugin.getPlacedCrateManager().register(placed);
        plugin.getAnimationEngine().startIdle(display);
        return true;
    }

    /** Remove por localizacao (fluxo antigo: mirando o bloco). */
    public boolean removeAt(Location location) {
        PlacedCrate placed = plugin.getPlacedCrateManager().getAt(location);
        if (placed == null) {
            return false;
        }
        return removePlaced(placed);
    }

    /** Remove remotamente por crate+tag (ex: "basica:AC-001") - nao precisa estar perto. */
    public boolean removeByTag(String crateId, String tag) {
        PlacedCrate placed = plugin.getPlacedCrateManager().getByTag(tag);
        if (placed == null || !placed.getCrate().getId().equalsIgnoreCase(crateId)) {
            return false;
        }
        return removePlaced(placed);
    }

    private boolean removePlaced(PlacedCrate placed) {
        plugin.getAnimationEngine().stopIdle(placed.getDisplay());
        Location location = placed.getLocation();
        placed.remove();
        plugin.getPlacedCrateManager().unregister(location);
        try {
            plugin.getCrateLocationRepository().delete(location);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao remover crate do banco", e);
        }
        return true;
    }

    /** Carrega todas as crates persistidas do banco. Crate sem tag (colocada antes dessa
     * versao existir) ganha uma retroativamente, gravada de volta no banco. */
    public void loadAll() {
        try {
            for (CrateLocation cl : plugin.getCrateLocationRepository().findAll()) {
                Crate crate = plugin.getCratesConfig().getCrate(cl.getCrateId());
                if (crate == null) {
                    continue;
                }
                Location location = cl.toLocation();
                if (location == null) {
                    continue;
                }
                String tag = cl.getTag();
                if (tag == null || tag.isBlank()) {
                    try {
                        tag = plugin.getCrateLocationRepository().nextTag();
                        plugin.getCrateLocationRepository().updateTag(location, tag);
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "Falha ao gerar tag retroativa pra " + cl.getCrateId(), e);
                        tag = "AC-???";
                    }
                }
                placeExisting(crate, location, tag);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao carregar crates persistidas", e);
        }
    }
}
