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
    private final NamespacedKey rollsPdc;

    public KeyService(AlkaCrates plugin, VirtualKeyManager virtualKeyManager) {
        this.plugin = plugin;
        this.virtualKeyManager = virtualKeyManager;
        this.keyPdc = new NamespacedKey(plugin, "key");
        this.rollsPdc = new NamespacedKey(plugin, "key-rolls");
    }

    /** Cria um item de key fisica com PDC alkacrates:key=<crate_id> - material/nome/lore configuraveis por crate (key.*). */
    public ItemStack createPhysicalKey(String crateId, int amount) {
        return createPhysicalKey(crateId, amount, 1);
    }

    /**
     * Key fisica que vale {@code rolls} rolagens independentes por unidade consumida
     * (1 = normal). Versao simplificada do PICK_ONE/PICK_TWO do DadaCratesPro
     * (auditado 2026-09-03): la e uma sessao de GUI multi-tick com selecao de slots
     * em branco + animacao de roleta antes de revelar - decisao deliberada do usuario
     * (2026-09-03) de NAO trazer essa sessao de volta (round 3 desse projeto ja
     * removeu multi-tick opening de proposito, depois de um bug de knockback vindo
     * exatamente dessa classe de complexidade). Mesmo resultado final (N premios por
     * 1 key), sem UI extra - CrateService#openCrate ja rola/entrega em loop, so
     * precisava saber quantas vezes rolar por key consumida.
     */
    public ItemStack createPhysicalKey(String crateId, int amount, int rolls) {
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
        int safeRolls = Math.max(1, rolls);
        if (safeRolls > 1) {
            lore.add(MiniMessage.miniMessage().deserialize("<!i><gray>Vale <yellow>" + safeRolls + "x</yellow> premios por uso"));
        }
        meta.lore(lore);

        meta.getPersistentDataContainer().set(keyPdc, PersistentDataType.STRING, crateId);
        if (safeRolls > 1) {
            meta.getPersistentDataContainer().set(rollsPdc, PersistentDataType.INTEGER, safeRolls);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Quantas rolagens essa key fisica vale (1 = normal, sem tag). */
    public int getKeyRolls(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 1;
        }
        Integer value = item.getItemMeta().getPersistentDataContainer().get(rollsPdc, PersistentDataType.INTEGER);
        return value == null ? 1 : Math.max(1, value);
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
        giveKey(player, crateId, amount, type, 1);
    }

    /** rolls>1 so tem efeito em key FISICA (virtual nao carrega a tag, ver createPhysicalKey/getKeyRolls). */
    public void giveKey(Player player, String crateId, int amount, KeyType type, int rolls) {
        if (type == KeyType.PHYSICAL) {
            player.getInventory().addItem(createPhysicalKey(crateId, amount, rolls));
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

    /** Consome uma key e retorna quantas rolagens ela vale (getKeyRolls da unidade
     * consumida - key virtual nao carrega essa tag, sempre vale 1). 0 = nao tinha key. */
    public int consumeKeyForRolls(Player player, String crateId, KeyType type) {
        if (type == KeyType.PHYSICAL) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (isPhysicalKey(item, crateId)) {
                    int rolls = getKeyRolls(item);
                    item.setAmount(item.getAmount() - 1);
                    return rolls;
                }
            }
            return 0;
        }
        return virtualKeyManager.consumeKey(player.getUniqueId(), crateId) ? 1 : 0;
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
