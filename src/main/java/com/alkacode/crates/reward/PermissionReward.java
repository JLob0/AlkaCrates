package com.alkacode.crates.reward;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Reward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

/** Recompensa de permissao temporaria (addPermission por um tempo). */
public final class PermissionReward implements RewardExecutor {

    private final AlkaCrates plugin;

    public PermissionReward(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, Reward reward) {
        // reward.command contem a permissao a conceder; reward.amount segundos
        String permission = reward.getCommand();
        if (permission == null || permission.isEmpty()) {
            return;
        }
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission(permission, true);
        if (reward.getAmount() > 0) {
            final java.util.UUID owner = player.getUniqueId();
            final PermissionAttachment toRemove = attachment;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player p = Bukkit.getPlayer(owner);
                if (p != null) {
                    p.removeAttachment(toRemove);
                }
            }, (long) (reward.getAmount() * 20));
        }
    }
}
