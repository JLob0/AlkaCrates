package com.alkacode.crates.reward;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Reward;
import com.alkacode.crates.hook.AdvancedEnchantmentsHook;
import com.alkacode.crates.hook.item.ItemHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Recompensa de item fisico, resolvida por ItemHook (ItemsAdder...) ou Material, com meta
 * aplicada por cima: display-name, lore, encantamentos, unbreakable, custom-model-data, glow
 * e item-flags. Tudo aditivo (so mexe no que estiver setado) - pra nao apagar a meta de itens
 * custom (IA etc).
 */
public final class ItemReward implements RewardExecutor {

    private final AlkaCrates plugin;

    public ItemReward(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    private ItemStack resolve(Reward reward) {
        Reward.ItemOptions opts = reward.getItemOptions();
        if (opts != null && opts.aeBook() != null) {
            Reward.AeBook book = opts.aeBook();
            ItemStack aeItem = AdvancedEnchantmentsHook.createEnchantmentBook(
                    plugin.getLogger(), book.enchant(), book.level(), book.success(), book.failure());
            if (aeItem != null) {
                aeItem.setAmount(Math.max(1, (int) reward.getAmount()));
                applyMeta(aeItem, reward);
                return aeItem;
            }
            // AE ausente/falhou/nome invalido - cai pro item generico (ENCHANTED_BOOK) abaixo,
            // ja logado em WARNING pelo hook. Melhor dar um livro cosmetico do que nada.
        }
        String raw = reward.getItem();
        if (raw == null) {
            return null;
        }
        ItemStack item = null;
        for (ItemHook hook : plugin.getItemHooks()) {
            if (hook.matches(raw)) {
                ItemStack resolved = hook.resolve(raw);
                if (resolved != null) {
                    item = resolved;
                    break;
                }
            }
        }
        if (item == null) {
            Material material = Material.matchMaterial(raw);
            if (material == null) {
                return null;
            }
            item = new ItemStack(material);
        }
        item.setAmount(Math.max(1, (int) reward.getAmount()));
        applyMeta(item, reward);
        return item;
    }

    /** Aplica nome/lore/encantamentos/etc do reward por cima do item base. */
    private void applyMeta(ItemStack item, Reward reward) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        MiniMessage mm = MiniMessage.miniMessage();

        if (reward.getDisplayName() != null && !reward.getDisplayName().isBlank()) {
            meta.displayName(mm.deserialize("<!i>" + reward.getDisplayName()));
        }

        Reward.ItemOptions opts = reward.getItemOptions();
        if (opts != null) {
            if (opts.lore() != null && !opts.lore().isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : opts.lore()) {
                    lore.add(mm.deserialize("<!i>" + line));
                }
                meta.lore(lore);
            }
            if (opts.enchantments() != null) {
                for (Map.Entry<String, Integer> e : opts.enchantments().entrySet()) {
                    Enchantment ench = resolveEnchant(e.getKey());
                    if (ench != null) {
                        // ignoreLevelRestriction=true: crates costumam dar niveis acima do vanilla
                        meta.addEnchant(ench, Math.max(1, e.getValue()), true);
                    } else {
                        plugin.getLogger().warning("Encantamento desconhecido no reward "
                                + reward.getId() + ": " + e.getKey());
                    }
                }
            }
            if (opts.unbreakable()) {
                meta.setUnbreakable(true);
            }
            if (opts.customModelData() != null) {
                meta.setCustomModelData(opts.customModelData());
            }
            if (opts.glow()) {
                meta.setEnchantmentGlintOverride(true);
            }
            if (opts.itemFlags() != null) {
                for (String flag : opts.itemFlags()) {
                    try {
                        meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        item.setItemMeta(meta);
    }

    /** Encantamento pela chave moderna (sharpness, unbreaking...) ou "namespace:key". Null se nao achar. */
    private Enchantment resolveEnchant(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            NamespacedKey key = name.contains(":") ? NamespacedKey.fromString(name)
                    : NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT));
            return key != null ? Registry.ENCHANTMENT.get(key) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void execute(Player player, Reward reward) {
        ItemStack item = resolve(reward);
        if (item == null) {
            return;
        }
        player.getInventory().addItem(item).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }

    @Override
    public ItemStack resolveDisplayItem(Reward reward) {
        return resolve(reward);
    }
}
