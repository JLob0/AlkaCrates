package com.alkacode.crates.listener;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.KeyType;
import com.alkacode.crates.crate.placement.PlacedCrate;
import com.alkacode.crates.display.CrateDisplay;
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

/** Detecta interacao com crates colocadas e abre com a key. */
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
                event.setCancelled(true);
                plugin.getCrateService().openCrate(event.getPlayer(), placed, KeyType.PHYSICAL);
                return;
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
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
        plugin.getCrateService().openCrate(player, nearest, KeyType.PHYSICAL);
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
