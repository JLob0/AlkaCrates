package com.alkacode.crates.reward;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Reward;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Recompensa de dinheiro em qualquer moeda do AlkaEconomy. */
public final class MoneyReward implements RewardExecutor {

    private final AlkaCrates plugin;

    public MoneyReward(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, Reward reward) {
        String currency = reward.getCurrency() != null ? reward.getCurrency() : "gold";
        plugin.getEconomyHook().deposit(player.getUniqueId(), currency, reward.getAmount());
    }

    @Override
    public ItemStack resolveDisplayItem(Reward reward) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        String currency = reward.getCurrency() != null ? reward.getCurrency() : "gold";
        String name = reward.getDisplayName() != null
                ? reward.getDisplayName()
                : "<yellow>" + (long) reward.getAmount() + " " + currency;
        meta.displayName(MiniMessage.miniMessage().deserialize("<!i>" + name));
        item.setItemMeta(meta);
        return item;
    }
}
