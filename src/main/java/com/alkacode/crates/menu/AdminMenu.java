package com.alkacode.crates.menu;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Menu administrativo: lista as crates cadastradas. View-only por enquanto. */
public final class AdminMenu extends BaseGui {

    private final AlkaCrates plugin;

    public AdminMenu(AlkaCrates plugin, Player player) {
        super(plugin, player, "<gradient:#FFD700:#FFA500>AlkaCrates - Admin</gradient>", 6, "alkacrates-admin");
        this.plugin = plugin;
    }

    @Override
    public void render() {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        fillBorder(border);

        int slot = 10;
        for (Crate crate : plugin.getCratesConfig().getCrates()) {
            if (slot >= 44) {
                break;
            }
            ItemStack item = new ItemStack(Material.CHEST);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(crate.getDisplayName()));
            meta.lore(java.util.List.of(
                    Component.text("Engine: " + crate.getEngineType()),
                    Component.text("Rewards: " + crate.getRewards().size()),
                    Component.text("Preco: " + crate.getPrice() + " " + crate.getPriceCurrency())));
            item.setItemMeta(meta);
            setItem(slot, item);
            slot++;
        }
    }
}
