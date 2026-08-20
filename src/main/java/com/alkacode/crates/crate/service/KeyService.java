package com.alkacode.crates.crate.service;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.KeyType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Gerencia keys fisicas (PDC) e virtuais (cache em memoria + banco, ver VirtualKeyManager). */
public final class KeyService {

    private final AlkaCrates plugin;
    private final VirtualKeyManager virtualKeyManager;
    private final NamespacedKey keyPdc;

    public KeyService(AlkaCrates plugin, VirtualKeyManager virtualKeyManager) {
        this.plugin = plugin;
        this.virtualKeyManager = virtualKeyManager;
        this.keyPdc = new NamespacedKey(plugin, "key");
    }

    /** Cria um item de key fisica com PDC alkacrates:key=<crate_id> - material/nome/lore configuraveis por crate (key.*). */
    public ItemStack createPhysicalKey(String crateId, int amount) {
        Crate crate = plugin.getCratesConfig().getCrate(crateId);
        Material material = crate != null ? org.bukkit.Material.matchMaterial(crate.getKeyMaterial()) : null;
        ItemStack item = new ItemStack(material != null ? material : Material.TRIPWIRE_HOOK, amount);
        ItemMeta meta = item.getItemMeta();

        String crateName = crate != null ? crate.getDisplayName() : crateId;
        String name = crate != null && crate.getKeyName() != null
                ? crate.getKeyName()
                : "<gradient:#FFD700:#FFA500>Key de " + crateName + "</gradient>";
        meta.displayName(MiniMessage.miniMessage().deserialize("<!i>" + name));

        List<Component> lore = new ArrayList<>();
        if (crate != null) {
            for (String line : crate.getKeyLore()) {
                lore.add(MiniMessage.miniMessage().deserialize("<!i>" + line));
            }
        }
        meta.lore(lore);

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

    /** Entrega key fisica (inventario) ou virtual (cache + banco). */
    public void giveKey(Player player, String crateId, int amount, KeyType type) {
        if (type == KeyType.PHYSICAL) {
            player.getInventory().addItem(createPhysicalKey(crateId, amount));
        } else {
            virtualKeyManager.addKeys(player.getUniqueId(), crateId, amount);
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
        return virtualKeyManager.consumeKey(player.getUniqueId(), crateId);
    }

    /** Deposita ate `amount` keys fisicas no saldo virtual (banco de key da mochila). Retorna quantas depositou de fato. */
    public int depositPhysical(Player player, String crateId, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (remaining <= 0) {
                break;
            }
            if (isPhysicalKey(item, crateId)) {
                int take = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - take);
                remaining -= take;
            }
        }
        int deposited = amount - remaining;
        if (deposited > 0) {
            virtualKeyManager.addKeys(player.getUniqueId(), crateId, deposited);
        }
        return deposited;
    }

    /** Saca ate `amount` keys virtuais como item fisico (sobra cai no chao se o inventario lotar). Retorna quantas sacou. */
    public int withdrawVirtual(Player player, String crateId, int amount) {
        int withdrawn = virtualKeyManager.removeKeys(player.getUniqueId(), crateId, amount);
        if (withdrawn > 0) {
            player.getInventory().addItem(createPhysicalKey(crateId, withdrawn)).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
        return withdrawn;
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
        return virtualKeyManager.getKeys(player.getUniqueId(), crateId);
    }
}
