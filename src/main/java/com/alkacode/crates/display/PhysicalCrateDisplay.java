package com.alkacode.crates.display;

import com.alkacode.crates.crate.model.Crate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;

/**
 * Display de crate com baú físico no chão e animação 3D flutuando acima.
 * O bloco original é restaurado ao remover. Interação com o baú abre a crate.
 */
public final class PhysicalCrateDisplay extends CrateDisplay {

    private final Block block;
    private final BlockState originalBlock;

    public PhysicalCrateDisplay(Crate crate, Location location, ItemStack item,
                                String hologram, float[] baseScale) {
        super(crate, location.clone().add(0, 1.5, 0), item, hologram, baseScale, 0.8f, 0.8f, 1.2, 0.5f);
        this.block = location.getBlock();
        this.originalBlock = block.getState();
        Material blockMaterial = Material.matchMaterial(crate.getBlockMaterial());
        block.setType(blockMaterial != null && blockMaterial.isBlock() ? blockMaterial : Material.CHEST, false);
    }

    @Override
    public void remove() {
        super.remove();
        originalBlock.update(true, false);
    }

    public Block getBlock() {
        return block;
    }
}
