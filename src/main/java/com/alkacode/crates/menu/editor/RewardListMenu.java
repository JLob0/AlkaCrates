package com.alkacode.crates.menu.editor;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.Reward;
import com.alkacode.crates.gui.layout.GuiLayoutLoader;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lista as recompensas de uma crate (paginada), adiciona novas e abre o editor de cada uma. */
public final class RewardListMenu extends BaseGui {

    private final AlkaCrates plugin;
    private final String crateId;
    private final int page;

    public RewardListMenu(AlkaCrates plugin, Player player, String crateId) {
        this(plugin, player, crateId, 0);
    }

    public RewardListMenu(AlkaCrates plugin, Player player, String crateId, int page) {
        super(plugin, player, plugin.getMenuConfig().title("alkacrates-reward-list.title", Map.of("crate", crateId)),
                6, "alkacrates-reward-list");
        this.plugin = plugin;
        this.crateId = crateId;
        this.page = page;
    }

    @Override
    public void render() {
        Crate crate = plugin.getCratesConfig().getCrate(crateId);
        if (crate == null) {
            player.closeInventory();
            return;
        }
        GuiLayoutLoader.GuiLayout layout = plugin.getGuiLayoutLoader().getLayout("alkacrates-reward-list");
        com.alkacode.crates.config.MenuConfig menu = plugin.getMenuConfig();
        fillBorder(menu.item("common.border", null));

        setItem(layout.firstSlot('B'), menu.item("alkacrates-reward-list.adicionar", null), event -> {
            YamlConfiguration config = plugin.getCrateFileService().load(crateId);
            String newId = plugin.getCrateFileService().addReward(config);
            plugin.getCrateFileService().saveAndReload(crateId, config);
            new RewardEditMenu(plugin, player, crateId, newId).open();
        });

        setItem(layout.firstSlot('V'), menu.item("alkacrates-reward-list.voltar", null),
                event -> new CrateEditMenu(plugin, player, crateId).open());

        List<Reward> rewards = crate.getRewards();
        double totalChance = rewards.stream().mapToDouble(Reward::getChance).sum();

        List<Integer> contentSlots = layout.findSlots('0');
        int start = page * contentSlots.size();
        if (page > 0 && start >= rewards.size()) {
            new RewardListMenu(plugin, player, crateId, 0).open();
            return;
        }
        if (page > 0) {
            setItem(layout.firstSlot('P'), menu.item("alkacrates-reward-list.pagina-anterior", null),
                    event -> new RewardListMenu(plugin, player, crateId, page - 1).open());
        }
        if (start + contentSlots.size() < rewards.size()) {
            setItem(layout.firstSlot('N'), menu.item("alkacrates-reward-list.proxima-pagina", null),
                    event -> new RewardListMenu(plugin, player, crateId, page + 1).open());
        }

        for (int i = 0; i < contentSlots.size() && start + i < rewards.size(); i++) {
            Reward reward = rewards.get(start + i);
            ItemStack resolved = plugin.getCrateService().getRewardDispatcher().resolveDisplayItem(reward);
            ItemStack icon = resolved != null ? resolved.clone() : new ItemStack(Material.BARRIER);
            double chance = totalChance > 0 ? (reward.getChance() / totalChance) * 100 : 0;
            String name = reward.getDisplayName() != null ? reward.getDisplayName() : reward.getId();
            List<String> lore = menu.rawLore("alkacrates-reward-list.reward-item", Map.of(
                    "tipo", reward.getType().toString(),
                    "chance", String.format(Locale.ROOT, "%.2f", chance),
                    "id", reward.getId()));
            setItem(contentSlots.get(i), withLore(icon, "<gold><bold>" + name, lore.toArray(new String[0])), event -> {
                if (event.getClick() == ClickType.DROP) {
                    YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                    plugin.getCrateFileService().removeReward(config, reward.getId());
                    plugin.getCrateFileService().saveAndReload(crateId, config);
                    new RewardListMenu(plugin, player, crateId, page).open();
                } else {
                    new RewardEditMenu(plugin, player, crateId, reward.getId()).open();
                }
            });
        }
    }

    /** Clona o item resolvido e substitui nome/lore, preservando o resto do meta (durabilidade, textura...). */
    private ItemStack withLore(ItemStack item, String name, String... lore) {
        var meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<!i>" + name));
        java.util.List<net.kyori.adventure.text.Component> loreList = new java.util.ArrayList<>();
        for (String line : lore) {
            loreList.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<!i>" + line));
        }
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }
}
