package com.alkacode.crates.reward;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Reward;
import org.bukkit.entity.Player;

/** Recompensa de dinheiro em qualquer moeda do AlkaEconomy. */
public final class MoneyReward implements RewardExecutor {

    private final AlkaCrates plugin;

    public MoneyReward(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, Reward reward) {
        String currency = reward.getCurrency() != null ? reward.getCurrency() : "coins";
        plugin.getEconomyHook().deposit(player.getUniqueId(), currency, reward.getAmount());
    }
}
