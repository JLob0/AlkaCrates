package com.alkacode.crates.listener;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.KeyType;
import com.alkacode.crates.crate.placement.PlacedCrate;
import com.alkacode.crates.display.CrateDisplay;
import com.alkacode.crates.engine.CrateEngineType;
import com.alkacode.crates.menu.PreviewMenu;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Detecta interacao com crates colocadas. Esquerdo abre (shift = todas as keys de
 * uma vez), direito mostra o preview - so pra PHYSICAL_CHEST, que tem um bloco de
 * verdade pra distinguir os dois botoes com seguranca (esquerdo-em-entidade sempre
 * vira ataque no Minecraft, entao os outros engines - so entidade, sem bloco -
 * continuam no botao direito).
 */
public final class CrateInteractionListener implements Listener {

    private final AlkaCrates plugin;

    public CrateInteractionListener(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Interaction interaction)) {
            return;
        }
        for (PlacedCrate placed : plugin.getPlacedCrateManager().getAll()) {
            CrateDisplay display = placed.getDisplay();
            if (display != null && interaction.equals(display.getInteraction())) {
                if (placed.getCrate().getEngineType() == CrateEngineType.PHYSICAL_CHEST) {
                    // o bloco cuida da interacao pra esse engine - o item flutuante e so decorativo.
                    return;
                }
                event.setCancelled(true);
                plugin.getCrateService().openCrate(event.getPlayer(), placed, KeyType.PHYSICAL, event.getPlayer().isSneaking());
                return;
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        if (!left && !right) {
            return;
        }
        Player player = event.getPlayer();

        if (event.getClickedBlock() != null) {
            Location blockLoc = event.getClickedBlock().getLocation().add(0.5, 0, 0.5);
            PlacedCrate blockCrate = plugin.getPlacedCrateManager().getAt(blockLoc);
            if (blockCrate != null && blockCrate.getCrate().getEngineType() == CrateEngineType.PHYSICAL_CHEST) {
                event.setCancelled(true);
                if (right) {
                    new PreviewMenu(plugin, player, blockCrate.getCrate()).open();
                } else {
                    plugin.getCrateService().openCrate(player, blockCrate, KeyType.PHYSICAL, player.isSneaking());
                }
                return;
            }
        }

        // segurando uma key fisica: esquerdo (perto de qualquer crate, ate 5 blocos) abre.
        if (!left) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        String crateId = plugin.getKeyService().getKeyCrateId(item);
        if (crateId == null) {
            return;
        }
        event.setCancelled(true);
        PlacedCrate nearest = findNearest(player.getLocation(), 5);
        if (nearest == null) {
            plugin.getCratesMessages().send(player, "crate-no-key", java.util.Map.of("crate", crateId));
            return;
        }
        plugin.getCrateService().openCrate(player, nearest, KeyType.PHYSICAL, player.isSneaking());
    }

    private PlacedCrate findNearest(Location center, double radius) {
        PlacedCrate nearest = null;
        double best = radius * radius;
        for (PlacedCrate placed : plugin.getPlacedCrateManager().getAll()) {
            if (!placed.getLocation().getWorld().equals(center.getWorld())) {
                continue;
            }
            double dist = placed.getLocation().distanceSquared(center);
            if (dist <= best) {
                best = dist;
                nearest = placed;
            }
        }
        return nearest;
    }
}
