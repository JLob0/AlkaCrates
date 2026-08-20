package com.alkacode.crates.menu.editor;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.engine.CrateEngineType;
import com.alkacode.crates.menu.AdminMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Editor de uma crate: nome, engine, item de exibicao, escala, preco e link pras recompensas. */
public final class CrateEditMenu extends BaseGui {

    private final AlkaCrates plugin;
    private final String crateId;

    public CrateEditMenu(AlkaCrates plugin, Player player, String crateId) {
        super(plugin, player, title(crateId), 4, "alkacrates-crate-edit");
        this.plugin = plugin;
        this.crateId = crateId;
    }

    private static String title(String crateId) {
        return "<gradient:#FFD700:#FFA500>Editar: " + crateId;
    }

    @Override
    public void render() {
        Crate crate = plugin.getCratesConfig().getCrate(crateId);
        if (crate == null) {
            player.closeInventory();
            return;
        }
        fillBorder(createItem(Material.BLACK_STAINED_GLASS_PANE, " "));

        setItem(10, createItem(Material.NAME_TAG, "<yellow><bold>Nome de exibicao",
                "<gray>Atual: <white>" + crate.getDisplayName(),
                "", "<yellow>Clique pra digitar o novo nome no chat"), event -> {
            promptText("crate-prompt-name", input -> {
                YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                config.set("display.name", input);
                plugin.getCrateFileService().saveAndReload(crateId, config);
                new CrateEditMenu(plugin, player, crateId).open();
            });
        });

        setItem(11, createItem(Material.BEACON, "<aqua><bold>Engine",
                "<gray>Atual: <white>" + crate.getEngineType(),
                "", "<yellow>Clique pra alternar"), event -> {
            CrateEngineType[] values = CrateEngineType.values();
            int next = (crate.getEngineType().ordinal() + 1) % values.length;
            YamlConfiguration config = plugin.getCrateFileService().load(crateId);
            config.set("display.engine", values[next].name());
            plugin.getCrateFileService().saveAndReload(crateId, config);
            refresh();
        });

        ItemStack currentDisplayIcon = crate.getCustomDisplayItem() != null
                ? crate.getCustomDisplayItem().clone()
                : new ItemStack(Material.matchMaterial(crate.getVanillaItem()) != null
                        ? Material.matchMaterial(crate.getVanillaItem()) : Material.BARRIER);
        setItem(12, withLore(currentDisplayIcon, "<light_purple><bold>Item de exibicao",
                "<gray>Atual: <white>" + (crate.getCustomDisplayItem() != null ? "item custom (arrastado)" : crate.getVanillaItem()),
                "", "<yellow>Arraste um item do seu inventario aqui",
                "<gray>(funciona com item custom - ItemsAdder,", "<gray>NBT, etc) ou clique vazio pra digitar",
                "<gray>um Material vanilla no chat"), event -> {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                config.set("display.custom-item", cursor.clone());
                plugin.getCrateFileService().saveAndReload(crateId, config);
                refresh();
                return;
            }
            promptText("crate-prompt-item", input -> {
                String material = input.trim().toUpperCase(Locale.ROOT);
                if (Material.matchMaterial(material) == null) {
                    player.sendMessage(plugin.getCratesMessages().parse("crate-invalid-material"));
                    new CrateEditMenu(plugin, player, crateId).open();
                    return;
                }
                YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                config.set("display.vanilla.item", material);
                config.set("display.custom-item", null);
                plugin.getCrateFileService().saveAndReload(crateId, config);
                new CrateEditMenu(plugin, player, crateId).open();
            });
        });

        double[] scale = crate.getScale();
        setItem(13, createItem(Material.ARMOR_STAND, "<gold><bold>Escala",
                "<gray>Atual: <white>" + scale[0] + ", " + scale[1] + ", " + scale[2],
                "", "<green>Esquerdo: <white>maior  <red>Direito: <white>menor"), event -> {
            double[] presets = {0.5, 0.8, 1.0, 1.2, 1.5, 2.0, 3.0};
            int idx = closestIndex(presets, scale[0]);
            int next = event.isLeftClick() ? Math.min(presets.length - 1, idx + 1) : Math.max(0, idx - 1);
            double value = presets[next];
            YamlConfiguration config = plugin.getCrateFileService().load(crateId);
            config.set("display.vanilla.scale", List.of(value, value, value));
            plugin.getCrateFileService().saveAndReload(crateId, config);
            refresh();
        });

        setItem(14, createItem(Material.EMERALD, "<green><bold>Preco",
                "<gray>Atual: <white>" + crate.getPrice() + " " + crate.getPriceCurrency(),
                "", "<green>Esquerdo: <white>+10 (shift +100)",
                "<red>Direito: <white>-10 (shift -100)",
                "<gray>0 = nao compravel via /crate buy"), event -> {
            double delta = event.isLeftClick() ? (event.isShiftClick() ? 100 : 10) : (event.isShiftClick() ? -100 : -10);
            double value = Math.max(0, crate.getPrice() + delta);
            YamlConfiguration config = plugin.getCrateFileService().load(crateId);
            config.set("price.amount", value);
            plugin.getCrateFileService().saveAndReload(crateId, config);
            refresh();
        });

        setItem(15, createItem(Material.SUNFLOWER, "<yellow><bold>Moeda",
                "<gray>Atual: <white>" + crate.getPriceCurrency(),
                "", "<yellow>Clique pra digitar a moeda no chat"), event -> {
            promptText("crate-prompt-currency", input -> {
                String currency = input.trim().toLowerCase(Locale.ROOT);
                if (!plugin.getEconomyHook().isValidCurrency(currency)) {
                    player.sendMessage(plugin.getCratesMessages().parse("crate-invalid-currency"));
                    new CrateEditMenu(plugin, player, crateId).open();
                    return;
                }
                YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                config.set("price.currency", currency);
                plugin.getCrateFileService().saveAndReload(crateId, config);
                new CrateEditMenu(plugin, player, crateId).open();
            });
        });

        setItem(19, createItem(Material.WRITABLE_BOOK, "<yellow><bold>Lore da caixa",
                "<gray>Atual: <white>" + (crate.getDisplayLore().isEmpty() ? "(nenhuma)" : String.join(" / ", crate.getDisplayLore())),
                "", "<gray>Aparece embaixo do nome no holograma",
                "<yellow>Clique pra digitar no chat (linhas separadas por |)"), event ->
                promptText("crate-prompt-lore", input -> {
                    YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                    config.set("display.lore", splitLines(input));
                    plugin.getCrateFileService().saveAndReload(crateId, config);
                    new CrateEditMenu(plugin, player, crateId).open();
                }));

        Material keyIcon = Material.matchMaterial(crate.getKeyMaterial());
        setItem(20, createItem(keyIcon != null ? keyIcon : Material.TRIPWIRE_HOOK, "<light_purple><bold>Material da key",
                "<gray>Atual: <white>" + crate.getKeyMaterial(),
                "", "<yellow>Clique pra digitar o Material no chat"), event ->
                promptText("crate-prompt-item", input -> {
                    String material = input.trim().toUpperCase(Locale.ROOT);
                    if (Material.matchMaterial(material) == null) {
                        player.sendMessage(plugin.getCratesMessages().parse("crate-invalid-material"));
                        new CrateEditMenu(plugin, player, crateId).open();
                        return;
                    }
                    YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                    config.set("key.material", material);
                    plugin.getCrateFileService().saveAndReload(crateId, config);
                    new CrateEditMenu(plugin, player, crateId).open();
                }));

        setItem(21, createItem(Material.NAME_TAG, "<light_purple><bold>Nome da key",
                "<gray>Atual: <white>" + (crate.getKeyName() != null ? crate.getKeyName() : "(padrao: Key de " + crate.getDisplayName() + ")"),
                "", "<yellow>Clique pra digitar no chat"), event ->
                promptText("crate-prompt-name", input -> {
                    YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                    config.set("key.name", input);
                    plugin.getCrateFileService().saveAndReload(crateId, config);
                    new CrateEditMenu(plugin, player, crateId).open();
                }));

        setItem(22, createItem(Material.WRITTEN_BOOK, "<light_purple><bold>Lore da key",
                "<gray>Atual: <white>" + (crate.getKeyLore().isEmpty() ? "(nenhuma)" : String.join(" / ", crate.getKeyLore())),
                "", "<yellow>Clique pra digitar no chat (linhas separadas por |)"), event ->
                promptText("crate-prompt-lore", input -> {
                    YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                    config.set("key.lore", splitLines(input));
                    plugin.getCrateFileService().saveAndReload(crateId, config);
                    new CrateEditMenu(plugin, player, crateId).open();
                }));

        Material blockIcon = Material.matchMaterial(crate.getBlockMaterial());
        setItem(23, createItem(blockIcon != null && blockIcon.isBlock() ? blockIcon : Material.CHEST,
                "<gold><bold>Bloco fisico",
                "<gray>Atual: <white>" + crate.getBlockMaterial(),
                "", "<gray>So vale pro engine PHYSICAL_CHEST",
                "<yellow>Clique pra digitar o Material no chat"), event ->
                promptText("crate-prompt-item", input -> {
                    String material = input.trim().toUpperCase(Locale.ROOT);
                    Material matched = Material.matchMaterial(material);
                    if (matched == null || !matched.isBlock()) {
                        player.sendMessage(plugin.getCratesMessages().parse("crate-invalid-material"));
                        new CrateEditMenu(plugin, player, crateId).open();
                        return;
                    }
                    YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                    config.set("display.block", material);
                    plugin.getCrateFileService().saveAndReload(crateId, config);
                    new CrateEditMenu(plugin, player, crateId).open();
                }));

        setItem(16, createItem(Material.CHEST, "<gold><bold>Recompensas",
                "<gray>Total: <white>" + crate.getRewards().size(),
                "", "<yellow>Clique pra gerenciar"),
                event -> new RewardListMenu(plugin, player, crateId).open());

        setItem(28, createItem(Material.BARRIER, "<red><bold>Deletar Crate",
                "<gray>Remove o arquivo YAML e as", "<gray>localizacoes fisicas ja colocadas.",
                "", "<red>Shift + botao direito pra confirmar"), event -> {
            if (event.isShiftClick() && event.isRightClick()) {
                plugin.getCrateFileService().delete(crateId);
                plugin.reloadEverything();
                player.sendMessage(plugin.getCratesMessages().parse("crate-delete-success",
                        Map.of("crate", crateId)));
                new AdminMenu(plugin, player).open();
            }
        });

        setItem(31, createItem(Material.ARROW, "<red><bold>Voltar", "<gray>Volta pra lista de crates"),
                event -> new AdminMenu(plugin, player).open());
    }

    private void promptText(String messageKey, java.util.function.Consumer<String> onInput) {
        player.closeInventory();
        player.sendMessage(plugin.getCratesMessages().parse(messageKey));
        plugin.getChatInputManager().await(player.getUniqueId(), input -> {
            if (input.equalsIgnoreCase("cancelar")) {
                new CrateEditMenu(plugin, player, crateId).open();
                return;
            }
            onInput.accept(input);
        });
    }

    /** "linha 1 | linha 2 | linha 3" no chat -> lista de linhas de lore. Vazio/"nenhuma" = sem lore. */
    private List<String> splitLines(String input) {
        if (input == null || input.isBlank() || input.equalsIgnoreCase("nenhuma")) {
            return List.of();
        }
        return java.util.Arrays.stream(input.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
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

    private int closestIndex(double[] values, double target) {
        int best = 0;
        double bestDiff = Double.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            double diff = Math.abs(values[i] - target);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = i;
            }
        }
        return best;
    }
}
