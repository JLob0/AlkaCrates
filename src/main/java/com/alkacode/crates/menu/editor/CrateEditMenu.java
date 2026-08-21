package com.alkacode.crates.menu.editor;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.engine.CrateEngineType;
import com.alkacode.crates.gui.layout.GuiLayoutLoader;
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
        super(plugin, player, plugin.getMenuConfig().title("alkacrates-crate-edit.title", Map.of("crate", crateId)),
                4, "alkacrates-crate-edit");
        this.plugin = plugin;
        this.crateId = crateId;
    }

    @Override
    public void render() {
        Crate crate = plugin.getCratesConfig().getCrate(crateId);
        if (crate == null) {
            player.closeInventory();
            return;
        }
        GuiLayoutLoader.GuiLayout layout = plugin.getGuiLayoutLoader().getLayout("alkacrates-crate-edit");
        com.alkacode.crates.config.MenuConfig menu = plugin.getMenuConfig();
        fillBorder(menu.item("common.border", null));

        setItem(layout.firstSlot('N'), menu.item("alkacrates-crate-edit.nome", Map.of("atual", crate.getDisplayName())), event -> {
            promptText("crate-prompt-name", input -> {
                YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                config.set("display.name", input);
                plugin.getCrateFileService().saveAndReload(crateId, config);
                new CrateEditMenu(plugin, player, crateId).open();
            });
        });

        setItem(layout.firstSlot('E'), menu.item("alkacrates-crate-edit.engine", Map.of("atual", crate.getEngineType().toString())), event -> {
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
        String itemAtual = crate.getCustomDisplayItem() != null ? "item custom (arrastado)" : crate.getVanillaItem();
        List<String> itemLore = menu.rawLore("alkacrates-crate-edit.item", Map.of("atual", itemAtual));
        setItem(layout.firstSlot('I'), withLore(currentDisplayIcon,
                menu.name("alkacrates-crate-edit.item", null), itemLore.toArray(new String[0])), event -> {
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
        setItem(layout.firstSlot('S'), menu.item("alkacrates-crate-edit.escala",
                Map.of("atual", scale[0] + ", " + scale[1] + ", " + scale[2])), event -> {
            double[] presets = {0.5, 0.8, 1.0, 1.2, 1.5, 2.0, 3.0};
            int idx = closestIndex(presets, scale[0]);
            int next = event.isLeftClick() ? Math.min(presets.length - 1, idx + 1) : Math.max(0, idx - 1);
            double value = presets[next];
            YamlConfiguration config = plugin.getCrateFileService().load(crateId);
            config.set("display.vanilla.scale", List.of(value, value, value));
            plugin.getCrateFileService().saveAndReload(crateId, config);
            refresh();
        });

        setItem(layout.firstSlot('P'), menu.item("alkacrates-crate-edit.preco",
                Map.of("atual", crate.getPrice() + " " + crate.getPriceCurrency())), event -> {
            double delta = event.isLeftClick() ? (event.isShiftClick() ? 100 : 10) : (event.isShiftClick() ? -100 : -10);
            double value = Math.max(0, crate.getPrice() + delta);
            YamlConfiguration config = plugin.getCrateFileService().load(crateId);
            config.set("price.amount", value);
            plugin.getCrateFileService().saveAndReload(crateId, config);
            refresh();
        });

        setItem(layout.firstSlot('M'), menu.item("alkacrates-crate-edit.moeda", Map.of("atual", crate.getPriceCurrency())), event -> {
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

        String loreAtual = crate.getDisplayLore().isEmpty() ? "(nenhuma)" : String.join(" / ", crate.getDisplayLore());
        setItem(layout.firstSlot('L'), menu.item("alkacrates-crate-edit.lore-caixa", Map.of("atual", loreAtual)), event ->
                promptText("crate-prompt-lore", input -> {
                    YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                    config.set("display.lore", splitLines(input));
                    plugin.getCrateFileService().saveAndReload(crateId, config);
                    new CrateEditMenu(plugin, player, crateId).open();
                }));

        Material keyIcon = Material.matchMaterial(crate.getKeyMaterial());
        List<String> keyMaterialLore = menu.rawLore("alkacrates-crate-edit.key-material", Map.of("atual", crate.getKeyMaterial()));
        setItem(layout.firstSlot('K'), withLore(new ItemStack(keyIcon != null ? keyIcon : Material.TRIPWIRE_HOOK),
                menu.name("alkacrates-crate-edit.key-material", null), keyMaterialLore.toArray(new String[0])), event ->
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

        String keyNomeAtual = crate.getKeyName() != null ? crate.getKeyName() : "(padrao: Key de " + crate.getDisplayName() + ")";
        setItem(layout.firstSlot('J'), menu.item("alkacrates-crate-edit.key-nome", Map.of("atual", keyNomeAtual)), event ->
                promptText("crate-prompt-name", input -> {
                    YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                    config.set("key.name", input);
                    plugin.getCrateFileService().saveAndReload(crateId, config);
                    new CrateEditMenu(plugin, player, crateId).open();
                }));

        String keyLoreAtual = crate.getKeyLore().isEmpty() ? "(nenhuma)" : String.join(" / ", crate.getKeyLore());
        setItem(layout.firstSlot('O'), menu.item("alkacrates-crate-edit.key-lore", Map.of("atual", keyLoreAtual)), event ->
                promptText("crate-prompt-lore", input -> {
                    YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                    config.set("key.lore", splitLines(input));
                    plugin.getCrateFileService().saveAndReload(crateId, config);
                    new CrateEditMenu(plugin, player, crateId).open();
                }));

        Material blockIcon = Material.matchMaterial(crate.getBlockMaterial());
        List<String> blockLore = menu.rawLore("alkacrates-crate-edit.bloco-fisico", Map.of("atual", crate.getBlockMaterial()));
        setItem(layout.firstSlot('F'), withLore(new ItemStack(blockIcon != null && blockIcon.isBlock() ? blockIcon : Material.CHEST),
                menu.name("alkacrates-crate-edit.bloco-fisico", null), blockLore.toArray(new String[0])), event ->
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

        setItem(layout.firstSlot('R'), menu.item("alkacrates-crate-edit.recompensas",
                        Map.of("atual", String.valueOf(crate.getRewards().size()))),
                event -> new RewardListMenu(plugin, player, crateId).open());

        setItem(layout.firstSlot('D'), menu.item("alkacrates-crate-edit.deletar", null), event -> {
            if (event.isShiftClick() && event.isRightClick()) {
                plugin.getCrateFileService().delete(crateId);
                plugin.reloadEverything();
                player.sendMessage(plugin.getCratesMessages().parse("crate-delete-success",
                        Map.of("crate", crateId)));
                new AdminMenu(plugin, player).open();
            }
        });

        setItem(layout.firstSlot('V'), menu.item("alkacrates-crate-edit.voltar", null),
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
