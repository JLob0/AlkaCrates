package com.alkacode.crates.crate.service;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.KeyType;
import com.alkacode.crates.repository.VirtualKeyRepository;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.logging.Level;

/** Gerencia keys fisicas (PDC) e virtuais (banco). */
public final class KeyService {

    private final AlkaCrates plugin;
    private final VirtualKeyRepository virtualKeyRepository;
    private final NamespacedKey keyPdc;

    public KeyService(AlkaCrates plugin, VirtualKeyRepository virtualKeyRepository) {
        this.plugin = plugin;
        this.virtualKeyRepository = virtualKeyRepository;
        this.keyPdc = new NamespacedKey(plugin, "key");
    }

    /** Cria um item de key fisica com PDC alkacrates:key=<crate_id>. */
    public ItemStack createPhysicalKey(String crateId, int amount) {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK, amount);
        ItemMeta meta = item.getItemMeta();
        String crateName = plugin.getCratesConfig().getCrate(crateId) != null
                ? plugin.getCratesConfig().getCrate(crateId).getDisplayName()
                : crateId;
        meta.displayName(MiniMessage.miniMessage().deserialize(
                "<!i><gradient:#FFD700:#FFA500>Key de " + crateName + "</gradient>"));
        meta.getPersistentDataContainer().set(keyPdc, PersistentDataType.STRING, crateId);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isPhysicalKey(ItemStack item, String crateId) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        String value = item.getItemMeta().getPersistentDataContainer().get(keyPdc, PersistentDataType.STRING);
        return value != null && value.equals(crateId);
    }

    public String getKeyCrateId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(keyPdc, PersistentDataType.STRING);
    }

    /** Entrega key fisica (inventario) ou virtual (banco). */
    public void giveKey(Player player, String crateId, int amount, KeyType type) {
        if (type == KeyType.PHYSICAL) {
            player.getInventory().addItem(createPhysicalKey(crateId, amount));
        } else {
            try {
                virtualKeyRepository.addKeys(player.getUniqueId(), crateId, amount);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Falha ao dar key virtual", e);
            }
        }
    }

    /** Consome uma key. Retorna true se conseguiu. */
    public boolean consumeKey(Player player, String crateId, KeyType type) {
        if (type == KeyType.PHYSICAL) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (isPhysicalKey(item, crateId)) {
                    item.setAmount(item.getAmount() - 1);
                    return true;
                }
            }
            return false;
        }
        int count = getKeyCount(player, crateId, KeyType.VIRTUAL);
        if (count <= 0) {
            return false;
        }
        try {
            virtualKeyRepository.removeKeys(player.getUniqueId(), crateId, 1);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao consumir key virtual", e);
            return false;
        }
    }

    public int getKeyCount(Player player, String crateId, KeyType type) {
        if (type == KeyType.PHYSICAL) {
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (isPhysicalKey(item, crateId)) {
                    count += item.getAmount();
                }
            }
            return count;
        }
        try {
            return virtualKeyRepository.getKeys(player.getUniqueId(), crateId);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao consultar key virtual", e);
            return 0;
        }
    }
}
