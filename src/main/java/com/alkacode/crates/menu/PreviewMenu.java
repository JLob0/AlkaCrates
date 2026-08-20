package com.alkacode.crates.menu;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.Reward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Menu de preview do loot de uma crate. View-only. */
public final class PreviewMenu extends BaseGui {

    private final AlkaCrates plugin;
    private final Crate crate;

    public PreviewMenu(AlkaCrates plugin, Player player, Crate crate) {
        super(plugin, player, title(plugin, crate), 6, "alkacrates-preview");
        this.plugin = plugin;
        this.crate = crate;
    }

    private static String title(AlkaCrates plugin, Crate crate) {
        return "<gradient:#FFD700:#FFA500>Loot de " + crate.getDisplayName();
    }

    @Override
    public void render() {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        fillBorder(border);

        double totalChance = crate.getRewards().stream().mapToDouble(Reward::getChance).sum();

        int slot = 10;
        for (Reward reward : crate.getRewards()) {
            if (slot >= 44) {
                break;
            }
            ItemStack resolved = plugin.getCrateService().getRewardDispatcher().resolveDisplayItem(reward);
            ItemStack item = resolved != null ? resolved.clone() : new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            List<Component> lore = new ArrayList<>(meta.hasLore() && meta.lore() != null ? meta.lore() : List.of());
            double chance = totalChance > 0 ? (reward.getChance() / totalChance) * 100 : 0;
            lore.add(MiniMessage.miniMessage().deserialize(
                    "<!i><gray>Chance: <yellow>" + trim(chance) + "%"));
            if (reward.hasSoftPity()) {
                int attempts = plugin.getRewardPityManager().getAttempts(player.getUniqueId(), crate.getId(), reward.getId());
                double effective = Math.min(reward.getPityMaxChance(), reward.getChance() + reward.getPityIncrement() * attempts);
                double range = reward.getPityMaxChance() - reward.getChance();
                double fraction = range > 0 ? Math.max(0, Math.min(1.0, (effective - reward.getChance()) / range)) : 1.0;
                lore.add(MiniMessage.miniMessage().deserialize("<!i> "));
                lore.add(MiniMessage.miniMessage().deserialize("<!i><light_purple>▷ Chance acumulada:"));
                lore.add(MiniMessage.miniMessage().deserialize(
                        "<!i>" + pityBar(fraction) + " <white>(" + Math.round(fraction * 100) + "%)"));
            }
            if (reward.isGuaranteed() && plugin.getPityService().isEnabled()) {
                int opens = plugin.getPityService().getOpens(player, crate.getId());
                int required = plugin.getPityService().getRequiredOpens();
                double fraction = required > 0 ? Math.max(0, Math.min(1.0, (double) opens / required)) : 1.0;
                lore.add(MiniMessage.miniMessage().deserialize("<!i> "));
                lore.add(MiniMessage.miniMessage().deserialize("<!i><light_purple>▷ Recompensa Garantida:"));
                lore.add(MiniMessage.miniMessage().deserialize(
                        "<!i>" + pityBar(fraction) + " <white>(" + opens + "/" + required + ")"));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            setItem(slot, item);
            slot++;
        }
    }

    /** Mostra ate 4 casas decimais (sem zero sobrando) - 2 casas escondia a diferenca entre 0.001% e 0.01%. */
    private String trim(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        String formatted = String.format(Locale.ROOT, "%.4f", value);
        while (formatted.endsWith("0")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        if (formatted.endsWith(".")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    /** Barra de progresso "[■■■□□□□□□□]" pro soft pity - 10 segmentos, cheio = <green>, vazio = <dark_gray>. */
    private String pityBar(double fraction) {
        int segments = 10;
        int filled = (int) Math.round(fraction * segments);
        StringBuilder bar = new StringBuilder("<dark_gray>[");
        for (int i = 0; i < segments; i++) {
            bar.append(i < filled ? "<green>■" : "<dark_gray>▪");
        }
        bar.append("<dark_gray>]");
        return bar.toString();
    }
}
