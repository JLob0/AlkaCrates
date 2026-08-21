package com.alkacode.crates.menu;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.gui.layout.GuiLayoutLoader;
import com.alkacode.crates.menu.editor.CrateEditMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Menu administrativo: lista as crates cadastradas, cria novas e abre o editor de cada uma. */
public final class AdminMenu extends BaseGui {

    private final AlkaCrates plugin;

    public AdminMenu(AlkaCrates plugin, Player player) {
        super(plugin, player, "<gradient:#FFD700:#FFA500>AlkaCrates - Admin</gradient>", 6, "alkacrates-admin");
        this.plugin = plugin;
    }

    @Override
    public void render() {
        GuiLayoutLoader.GuiLayout layout = plugin.getGuiLayoutLoader().getLayout("alkacrates-admin");
        fillBorder(plugin.getMenuConfig().item("common.border", null));

        setItem(layout.firstSlot('B'), plugin.getMenuConfig().item("alkacrates-admin.criar", null), event -> {
            player.closeInventory();
            player.sendMessage(plugin.getCratesMessages().parse("crate-prompt-new-id"));
            plugin.getChatInputManager().await(player.getUniqueId(), input -> {
                if (input.equalsIgnoreCase("cancelar")) {
                    new AdminMenu(plugin, player).open();
                    return;
                }
                String id = plugin.getCrateFileService().validateNewId(input);
                if (id == null) {
                    player.sendMessage(plugin.getCratesMessages().parse("crate-create-invalid-id"));
                    new AdminMenu(plugin, player).open();
                    return;
                }
                var config = plugin.getCrateFileService().createTemplate(id);
                plugin.getCrateFileService().saveAndReload(id, config);
                player.sendMessage(plugin.getCratesMessages().parse("crate-create-success",
                        java.util.Map.of("crate", id)));
                new CrateEditMenu(plugin, player, id).open();
            });
        });

        List<Integer> slots = layout.findSlots('0');
        List<Crate> crates = plugin.getCratesConfig().getCrates();
        for (int i = 0; i < slots.size() && i < crates.size(); i++) {
            Crate crate = crates.get(i);
            ItemStack icon = crateIcon(crate);
            List<String> lore = plugin.getMenuConfig().rawLore("alkacrates-admin.crate-item", Map.of(
                    "id", crate.getId(),
                    "engine", crate.getEngineType().toString(),
                    "recompensas", String.valueOf(crate.getRewards().size()),
                    "preco", String.valueOf(crate.getPrice()),
                    "moeda", crate.getPriceCurrency()));
            setItem(slots.get(i), withLore(icon, "<gold><bold>" + crate.getDisplayName(), lore.toArray(new String[0])),
                    event -> new CrateEditMenu(plugin, player, crate.getId()).open());
        }
    }

    private ItemStack crateIcon(Crate crate) {
        if (crate.getCustomDisplayItem() != null) {
            return crate.getCustomDisplayItem().clone();
        }
        Material material = Material.matchMaterial(crate.getVanillaItem());
        return new ItemStack(material != null ? material : Material.CHEST);
    }

    /** Clona o icone e substitui nome/lore, preservando o resto do meta (custom NBT, textura...). */
    private ItemStack withLore(ItemStack item, String name, String... lore) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize("<!i>" + name));
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(MiniMessage.miniMessage().deserialize("<!i>" + line));
        }
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }
}
