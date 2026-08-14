package com.alkacode.crates.reward;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Reward;
import com.alkacode.crates.crate.model.RewardType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/** Roteia rewards para o executor certo por tipo. */
public final class RewardDispatcher {

    private final Map<RewardType, RewardExecutor> executors = new EnumMap<>(RewardType.class);

    public RewardDispatcher(AlkaCrates plugin) {
        executors.put(RewardType.ITEM, new ItemReward(plugin));
        executors.put(RewardType.MONEY, new MoneyReward(plugin));
        executors.put(RewardType.COMMAND, new CommandReward());
        executors.put(RewardType.VIP_DAYS, new VipReward());
        executors.put(RewardType.KIT, new KitReward());
        executors.put(RewardType.PERMISSION, new PermissionReward(plugin));
    }

    public void execute(Player player, Reward reward) {
        RewardExecutor executor = executors.get(reward.getType());
        if (executor != null) {
            executor.execute(player, reward);
        }
    }

    public ItemStack resolveDisplayItem(Reward reward) {
        RewardExecutor executor = executors.get(reward.getType());
        return executor != null ? executor.resolveDisplayItem(reward) : null;
    }
}
