package com.alkacode.crates.reward;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Reward;
import com.alkacode.crates.hook.item.ItemHook;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Recompensa de item fisico, resolvida por ItemHook (ItemsAdder...) ou Material. */
public final class ItemReward implements RewardExecutor {

    private final AlkaCrates plugin;

    public ItemReward(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    private ItemStack resolve(Reward reward) {
        String raw = reward.getItem();
        if (raw == null) {
            return null;
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
        if (material == null) {
            return null;
        }
        ItemStack item = new ItemStack(material);
        item.setAmount(Math.max(1, (int) reward.getAmount()));
        return item;
    }

    @Override
    public void execute(Player player, Reward reward) {
        ItemStack item = resolve(reward);
        if (item == null) {
            return;
        }
        player.getInventory().addItem(item).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }

    @Override
    public ItemStack resolveDisplayItem(Reward reward) {
        return resolve(reward);
    }
}
