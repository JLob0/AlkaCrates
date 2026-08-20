package com.alkacode.crates.engine;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.display.PhysicalCrateDisplay;
import com.alkacode.crates.hook.item.ItemHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Engine de baú físico: coloca um bloco de chest no mundo e um display 3D
 * flutuando acima para rodar a animação. Clicar no baú abre a crate.
 */
public final class PhysicalChestEngine implements CrateEngine {

    private final AlkaCrates plugin;

    public PhysicalChestEngine(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public com.alkacode.crates.display.CrateDisplay createDisplay(Crate crate, Location location) {
        ItemStack item = resolveDisplayItem(crate);
        float[] scale = new float[]{(float) crate.getScale()[0], (float) crate.getScale()[1], (float) crate.getScale()[2]};
        return new PhysicalCrateDisplay(crate, location, item, hologramText(crate), scale);
    }

    @Override
    public ItemStack resolveDisplayItem(Crate crate) {
        if (crate.getCustomDisplayItem() != null) {
            return crate.getCustomDisplayItem().clone();
        }
        String raw = crate.getVanillaItem();
        if (raw == null) {
            return new ItemStack(Material.CHEST);
        }
        for (ItemHook hook : plugin.getItemHooks()) {
            if (hook.matches(raw)) {
                ItemStack resolved = hook.resolve(raw);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        Material material = Material.matchMaterial(raw);
        return material != null ? new ItemStack(material) : new ItemStack(Material.CHEST);
    }

    @Override
    public CrateEngineType getType() {
        return CrateEngineType.PHYSICAL_CHEST;
    }

    @Override
    public List<String> interactionHints() {
        return List.of(
                "<yellow>➜ <gray>Esquerdo: <gold>Abrir caixa",
                "<yellow>➜ <gray>Direito: <gold>Recompensas");
    }
}
