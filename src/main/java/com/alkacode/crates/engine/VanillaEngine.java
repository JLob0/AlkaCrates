package com.alkacode.crates.engine;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.display.CrateDisplay;
import com.alkacode.crates.hook.item.ItemHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Engine vanilla baseada em display entities. Nao depende de nenhum plugin. */
public final class VanillaEngine implements CrateEngine {

    private final AlkaCrates plugin;

    public VanillaEngine(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public CrateDisplay createDisplay(Crate crate, Location location) {
        ItemStack item = resolveDisplayItem(crate);
        float[] scale = new float[]{(float) crate.getScale()[0], (float) crate.getScale()[1], (float) crate.getScale()[2]};
        return new CrateDisplay(crate, location, item, crate.getDisplayName(), scale, 0.8f, 0.8f);
    }

    @Override
    public ItemStack resolveDisplayItem(Crate crate) {
        String raw = crate.getVanillaItem();
        if (raw == null) {
            return new ItemStack(Material.DIAMOND_BLOCK);
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
        return material != null ? new ItemStack(material) : new ItemStack(Material.DIAMOND_BLOCK);
    }

    @Override
    public CrateEngineType getType() {
        return CrateEngineType.VANILLA;
    }
}
