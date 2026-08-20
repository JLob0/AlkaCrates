package com.alkacode.crates.menu.editor;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.Reward;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/** Lista as recompensas de uma crate (paginada), adiciona novas e abre o editor de cada uma. */
public final class RewardListMenu extends BaseGui {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final AlkaCrates plugin;
    private final String crateId;
    private final int page;

    public RewardListMenu(AlkaCrates plugin, Player player, String crateId) {
        this(plugin, player, crateId, 0);
    }

    public RewardListMenu(AlkaCrates plugin, Player player, String crateId, int page) {
        super(plugin, player, "<gradient:#FFD700:#FFA500>Recompensas: " + crateId, 6, "alkacrates-reward-list");
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
        fillBorder(createItem(Material.BLACK_STAINED_GLASS_PANE, " "));

        setItem(4, createItem(Material.LIME_DYE, "<green><bold>+ Adicionar Recompensa",
                "<gray>Cria uma recompensa ITEM padrao", "<gray>(voce edita os campos em seguida)"), event -> {
            YamlConfiguration config = plugin.getCrateFileService().load(crateId);
            String newId = plugin.getCrateFileService().addReward(config);
            plugin.getCrateFileService().saveAndReload(crateId, config);
            new RewardEditMenu(plugin, player, crateId, newId).open();
        });

        setItem(49, createItem(Material.ARROW, "<red><bold>Voltar", "<gray>Volta pro editor da crate"),
                event -> new CrateEditMenu(plugin, player, crateId).open());

        List<Reward> rewards = crate.getRewards();
        double totalChance = rewards.stream().mapToDouble(Reward::getChance).sum();

        int start = page * CONTENT_SLOTS.length;
        if (page > 0 && start >= rewards.size()) {
            new RewardListMenu(plugin, player, crateId, 0).open();
            return;
        }
        if (page > 0) {
            setItem(45, createItem(Material.ARROW, "<yellow><bold>Pagina anterior"),
                    event -> new RewardListMenu(plugin, player, crateId, page - 1).open());
        }
        if (start + CONTENT_SLOTS.length < rewards.size()) {
            setItem(53, createItem(Material.ARROW, "<yellow><bold>Proxima pagina"),
                    event -> new RewardListMenu(plugin, player, crateId, page + 1).open());
        }

        for (int i = 0; i < CONTENT_SLOTS.length && start + i < rewards.size(); i++) {
            Reward reward = rewards.get(start + i);
            ItemStack resolved = plugin.getCrateService().getRewardDispatcher().resolveDisplayItem(reward);
            ItemStack icon = resolved != null ? resolved.clone() : new ItemStack(Material.BARRIER);
            double chance = totalChance > 0 ? (reward.getChance() / totalChance) * 100 : 0;
            String name = reward.getDisplayName() != null ? reward.getDisplayName() : reward.getId();
            setItem(CONTENT_SLOTS[i], withLore(icon, "<gold><bold>" + name,
                    "<gray>Tipo: <white>" + reward.getType(),
                    "<gray>Chance: <yellow>" + String.format(Locale.ROOT, "%.2f", chance) + "%",
                    "<gray>ID: <white>" + reward.getId(),
                    "",
                    "<yellow>Clique pra editar",
                    "<dark_red>Solte (Q) pra remover"), event -> {
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
