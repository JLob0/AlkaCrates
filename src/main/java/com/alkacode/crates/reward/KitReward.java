package com.alkacode.crates.reward;

import com.alkacode.crates.crate.model.Reward;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Recompensa de kit. Integra com o AlkaKits via reflection best-effort: tenta
 * achar o KitManager e um metodo de grant. Se nao disponivel, loga aviso.
 */
public final class KitReward implements RewardExecutor {

    @Override
    public void execute(Player player, Reward reward) {
        Plugin kits = Bukkit.getPluginManager().getPlugin("AlkaKits");
        if (kits == null) {
            return;
        }
        try {
            Object manager = findManager(kits, "KitManager");
            if (manager == null) {
                kits.getLogger().warning("[AlkaCrates] KitManager do AlkaKits nao encontrado - reward KIT ignorado.");
                return;
            }
            Method getKit = manager.getClass().getMethod("getKit", String.class);
            Object kit = getKit.invoke(manager, reward.getKitId());
            if (kit == null) {
                kits.getLogger().warning("[AlkaCrates] Kit '" + reward.getKitId() + "' nao existe - reward KIT ignorado.");
                return;
            }
            // tenta grant via KitClaimService (best-effort)
            try {
                Object claimService = findManager(kits, "KitClaimService");
                if (claimService != null) {
                    for (Method m : claimService.getClass().getMethods()) {
                        if (m.getName().toLowerCase().contains("claim") && m.getParameterCount() >= 2) {
                            m.invoke(claimService, player, kit);
                            return;
                        }
                    }
                }
            } catch (IllegalAccessException ignored) {
            }
            kits.getLogger().warning("[AlkaCrates] Nenhum metodo de claim viavel encontrado no AlkaKits.");
        } catch (Throwable t) {
            kits.getLogger().log(Level.WARNING, "[AlkaCrates] Falha ao processar reward KIT", t);
        }
    }

    @Override
    public ItemStack resolveDisplayItem(Reward reward) {
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();
        String name = reward.getDisplayName() != null
                ? reward.getDisplayName()
                : "<aqua>Kit " + (reward.getKitId() != null ? reward.getKitId() : "");
        meta.displayName(MiniMessage.miniMessage().deserialize("<!i>" + name));
        item.setItemMeta(meta);
        return item;
    }

    private Object findManager(Plugin plugin, String simpleName) {
        try {
            for (Field field : plugin.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(plugin);
                if (value != null && value.getClass().getSimpleName().contains(simpleName)) {
                    return value;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
