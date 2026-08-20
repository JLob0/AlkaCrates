package com.alkacode.crates.reward;

import com.alkacode.crates.crate.model.Reward;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Recompensa por comando, com %player% substituido. Executado como console. */
public final class CommandReward implements RewardExecutor {

    @Override
    public void execute(Player player, Reward reward) {
        String command = reward.getCommand();
        if (command == null || command.isEmpty()) {
            return;
        }
        String resolved = command.replace("%player%", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
    }

    @Override
    public ItemStack resolveDisplayItem(Reward reward) {
        ItemStack item = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta meta = item.getItemMeta();
        String name = reward.getDisplayName() != null ? reward.getDisplayName() : "<gold>Recompensa especial";
        meta.displayName(MiniMessage.miniMessage().deserialize("<!i>" + name));
        item.setItemMeta(meta);
        return item;
    }
}
