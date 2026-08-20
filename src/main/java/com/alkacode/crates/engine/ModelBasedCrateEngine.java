package com.alkacode.crates.engine;

import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.display.CrateDisplay;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

/**
 * Engine generico pra qualquer sistema de modelo externo (ModelEngine/BetterModel/
 * CraftEngine/ItemsAdder furniture) - um so lugar em vez de 4 classes quase identicas.
 * `crate.getVanillaItem()` vira o ID do modelo (nao um Material) pra esses engines.
 * Nao spawna item flutuante vanilla (ver CrateDisplay - item null = "so modelo"), so a
 * hitbox/holograma + o modelo em si. Se o spawn falhar (plugin ausente ou API mudou),
 * a crate ainda fica valida (hitbox + holograma), so sem visual 3D - nunca falha o
 * placement inteiro por causa disso.
 */
public final class ModelBasedCrateEngine implements CrateEngine {

    private final ModelSpawner spawner;
    private final CrateEngineType type;

    public ModelBasedCrateEngine(ModelSpawner spawner, CrateEngineType type) {
        this.spawner = spawner;
        this.type = type;
    }

    @Override
    public CrateDisplay createDisplay(Crate crate, Location location) {
        CrateDisplay display = new CrateDisplay(crate, location, null, hologramText(crate),
                new float[]{1, 1, 1}, 0.8f, 1.2f);
        if (spawner.isAvailable()) {
            Entity model = spawner.spawn(crate.getVanillaItem(), location);
            if (model != null) {
                display.setModelEntity(model);
            }
        }
        return display;
    }

    @Override
    public ItemStack resolveDisplayItem(Crate crate) {
        // usado so em menus/preview (a crate no mundo usa o modelo de verdade). Se o admin
        // arrastou um item custom como icone, usa ele; senao cai num icone generico, ja que
        // crate.getVanillaItem() aqui e um ID de modelo, nao um Material.
        if (crate.getCustomDisplayItem() != null) {
            return crate.getCustomDisplayItem().clone();
        }
        return new ItemStack(Material.CHEST);
    }

    @Override
    public CrateEngineType getType() {
        return type;
    }
}
