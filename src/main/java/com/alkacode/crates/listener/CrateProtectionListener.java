package com.alkacode.crates.listener;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.placement.PlacedCrate;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Impede que crates colocadas sejam quebradas, explodidas ou empurradas.
 * A crate so pode ser removida por comando (/alkacrates remove).
 */
public final class CrateProtectionListener implements Listener {

    private final AlkaCrates plugin;

    public CrateProtectionListener(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (isCrateBlock(event.getBlock())) {
            event.setCancelled(true);
            event.setDropItems(false);
            plugin.getCratesMessages().send(event.getPlayer(), "crate-cant-break");
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isCrateBlock);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isCrateBlock);
    }

    @EventHandler
    public void onBurn(BlockBurnEvent event) {
        if (isCrateBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        event.getBlocks().removeIf(this::isCrateBlock);
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        event.getBlocks().removeIf(this::isCrateBlock);
    }

    private boolean isCrateBlock(Block block) {
        if (block == null) {
            return false;
        }
        for (PlacedCrate placed : plugin.getPlacedCrateManager().getAll()) {
            if (placed.getLocation().getWorld().equals(block.getWorld())
                    && placed.getLocation().getBlockX() == block.getX()
                    && placed.getLocation().getBlockY() == block.getY()
                    && placed.getLocation().getBlockZ() == block.getZ()) {
                return true;
            }
        }
        return false;
    }
}
