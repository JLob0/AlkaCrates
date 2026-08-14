package com.alkacode.crates.menu;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.Reward;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

        int slot = 10;
        for (Reward reward : crate.getRewards()) {
            if (slot >= 44) {
                break;
            }
            ItemStack item = plugin.getCrateService().getRewardDispatcher().resolveDisplayItem(reward);
            if (item == null) {
                item = new ItemStack(Material.BARRIER);
            }
            setItem(slot, item);
            slot++;
        }
    }
}
