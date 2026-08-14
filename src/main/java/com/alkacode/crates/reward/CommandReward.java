package com.alkacode.crates.reward;

import com.alkacode.crates.crate.model.Reward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
}
