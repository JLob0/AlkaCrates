package com.alkacode.crates.menu.editor;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.config.MenuConfig;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.Reward;
import com.alkacode.crates.crate.model.RewardType;
import com.alkacode.crates.gui.layout.GuiLayoutLoader;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Editor de uma recompensa - campos comuns (posicao/icone/texto em gui-layouts.yml/
 * menus.yml) + campos especificos por RewardType (slots Y/Z do layout, mas o
 * icone/label continua em Java porque depende do enum ativo - nao da pra
 * generalizar em menus.yml sem uma secao por tipo, e o RewardType ja e o dono
 * dessa variacao).
 */
public final class RewardEditMenu extends BaseGui {

    private final AlkaCrates plugin;
    private final String crateId;
    private final String rewardId;

    public RewardEditMenu(AlkaCrates plugin, Player player, String crateId, String rewardId) {
        super(plugin, player, plugin.getMenuConfig().title("alkacrates-reward-edit.title", Map.of("reward", rewardId)),
                6, "alkacrates-reward-edit");
        this.plugin = plugin;
        this.crateId = crateId;
        this.rewardId = rewardId;
    }

    @Override
    public void render() {
        Crate crate = plugin.getCratesConfig().getCrate(crateId);
        Reward reward = crate != null ? findReward(crate) : null;
        if (crate == null || reward == null) {
            new RewardListMenu(plugin, player, crateId).open();
            return;
        }
        GuiLayoutLoader.GuiLayout layout = plugin.getGuiLayoutLoader().getLayout("alkacrates-reward-edit");
        MenuConfig menu = plugin.getMenuConfig();
        fillBorder(menu.item("common.border", null));

        setItem(layout.firstSlot('T'), menu.item("alkacrates-reward-edit.tipo", Map.of("atual", reward.getType().toString())), event -> {
            RewardType[] values = RewardType.values();
            int next = (reward.getType().ordinal() + 1) % values.length;
            mutate(config -> config.set(path("type"), values[next].name()));
        });

        setItem(layout.firstSlot('N'), menu.item("alkacrates-reward-edit.nome",
                Map.of("atual", reward.getDisplayName() != null ? reward.getDisplayName() : "(nenhum)")),
                event -> promptText("crate-prompt-name", input -> mutate(config -> config.set(path("display-name"), input))));

        setItem(layout.firstSlot('C'), menu.item("alkacrates-reward-edit.chance", Map.of("atual", trim(reward.getChance()))), event -> {
            if (event.getClick() == ClickType.DROP) {
                promptNumber("crate-prompt-chance", value -> mutate(config -> config.set(path("chance"), Math.max(0, value))));
                return;
            }
            double delta = event.isLeftClick() ? (event.isShiftClick() ? 10 : 1) : (event.isShiftClick() ? -10 : -1);
            double value = Math.max(0, reward.getChance() + delta);
            mutate(config -> config.set(path("chance"), value));
        });

        setItem(layout.firstSlot('G'), menu.item(reward.isGuaranteed()
                ? "alkacrates-reward-edit.garantida-sim" : "alkacrates-reward-edit.garantida-nao", null),
                event -> mutate(config -> config.set(path("guaranteed"), !reward.isGuaranteed())));

        setItem(layout.firstSlot('B'), menu.item(reward.isBroadcast()
                ? "alkacrates-reward-edit.broadcast-sim" : "alkacrates-reward-edit.broadcast-nao", null),
                event -> mutate(config -> config.set(path("broadcast"), !reward.isBroadcast())));

        setItem(layout.firstSlot('W'), menu.item("alkacrates-reward-edit.limite-jogador",
                Map.of("atual", reward.getWinLimit() < 0 ? "ilimitado" : String.valueOf(reward.getWinLimit()))), event -> {
            int value = reward.getWinLimit() + (event.isLeftClick() ? 1 : -1);
            mutate(config -> config.set(path("win-limit"), Math.max(-1, value)));
        });

        setItem(layout.firstSlot('D'), menu.item("alkacrates-reward-edit.cooldown-limite",
                Map.of("atual", reward.getWinLimitCooldownSeconds() + "s")), event -> {
            long delta = event.isLeftClick() ? (event.isShiftClick() ? 3600 : 60) : (event.isShiftClick() ? -3600 : -60);
            long value = Math.max(0, reward.getWinLimitCooldownSeconds() + delta);
            mutate(config -> config.set(path("win-limit-cooldown"), value));
        });

        setItem(layout.firstSlot('X'), menu.item("alkacrates-reward-edit.limite-global",
                Map.of("atual", reward.getGlobalWinLimit() < 0 ? "ilimitado" : String.valueOf(reward.getGlobalWinLimit()))),
                event -> {
            int value = reward.getGlobalWinLimit() + (event.isLeftClick() ? 1 : -1);
            mutate(config -> config.set(path("global-win-limit"), Math.max(-1, value)));
        });

        setItem(layout.firstSlot('P'), menu.item("alkacrates-reward-edit.permissoes-obrigatorias",
                Map.of("atual", joinOrNone(reward.getRequiredPermissions()))),
                event -> promptText("crate-prompt-permissions", input -> mutate(config ->
                        config.set(path("required-permissions"), splitPermissions(input)))));

        setItem(layout.firstSlot('R'), menu.item("alkacrates-reward-edit.permissoes-restritas",
                Map.of("atual", joinOrNone(reward.getRestrictedPermissions()))),
                event -> promptText("crate-prompt-permissions", input -> mutate(config ->
                        config.set(path("restricted-permissions"), splitPermissions(input)))));

        int attempts = plugin.getRewardPityManager().getAttempts(player.getUniqueId(), crateId, reward.getId());
        double effective = reward.hasSoftPity()
                ? Math.min(reward.getPityMaxChance(), reward.getChance() + reward.getPityIncrement() * attempts)
                : reward.getChance();
        setItem(layout.firstSlot('S'), menu.item("alkacrates-reward-edit.pity-incremento", Map.of("atual",
                reward.hasSoftPity() ? "+" + trim(reward.getPityIncrement()) + " por tentativa" : "desligado")), event -> {
            if (event.getClick() == ClickType.DROP) {
                promptNumber("crate-prompt-pity-increment", value -> mutate(config -> config.set(path("pity-increment"), Math.max(0, value))));
                return;
            }
            double delta = event.isLeftClick() ? (event.isShiftClick() ? 1 : 0.01) : (event.isShiftClick() ? -1 : -0.01);
            double value = Math.max(0, roundTwo(reward.getPityIncrement() + delta));
            mutate(config -> config.set(path("pity-increment"), value));
        });

        setItem(layout.firstSlot('M'), menu.item("alkacrates-reward-edit.pity-teto", Map.of(
                "atual", trim(reward.getPityMaxChance()),
                "efetiva", trim(effective),
                "tentativas", String.valueOf(attempts))), event -> {
            if (event.getClick() == ClickType.DROP) {
                promptNumber("crate-prompt-pity-cap", value -> mutate(config -> config.set(path("pity-max-chance"), Math.max(0, value))));
                return;
            }
            double delta = event.isLeftClick() ? (event.isShiftClick() ? 10 : 1) : (event.isShiftClick() ? -10 : -1);
            double value = Math.max(0, reward.getPityMaxChance() + delta);
            mutate(config -> config.set(path("pity-max-chance"), value));
        });

        renderTypeSpecific(reward, layout);

        setItem(layout.firstSlot('E'), menu.item("alkacrates-reward-edit.deletar", null), event -> {
            if (event.isShiftClick() && event.isRightClick()) {
                YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                plugin.getCrateFileService().removeReward(config, rewardId);
                plugin.getCrateFileService().saveAndReload(crateId, config);
                new RewardListMenu(plugin, player, crateId).open();
            }
        });

        setItem(layout.firstSlot('V'), menu.item("alkacrates-reward-edit.voltar", null),
                event -> new RewardListMenu(plugin, player, crateId).open());
    }

    /** Campos especificos do RewardType (slots Y/Z) - icone/label ficam em Java porque
     * dependem de qual dos 7 tipos esta ativo, nao sao chrome fixo de menu. */
    private void renderTypeSpecific(Reward reward, GuiLayoutLoader.GuiLayout layout) {
        int slotY = layout.firstSlot('Y');
        int slotZ = layout.firstSlot('Z');
        switch (reward.getType()) {
            case ITEM -> {
                Material icon = reward.getItem() != null ? Material.matchMaterial(reward.getItem()) : null;
                setItem(slotY, createItem(icon != null ? icon : Material.BARRIER, "<white><bold>Item",
                        "<gray>Atual: <white>" + reward.getItem(),
                        "", "<yellow>Clique pra digitar Material/ID no chat"),
                        event -> promptText("crate-prompt-item", input ->
                                mutate(config -> config.set(path("item"), input.trim()))));
                setItem(slotZ, createItem(Material.HOPPER, "<white><bold>Quantidade",
                        "<gray>Atual: <white>" + (long) reward.getAmount(),
                        "", "<green>Esquerdo: <white>+1 (shift +8)", "<red>Direito: <white>-1 (shift -8)"), event -> {
                    double delta = event.isLeftClick() ? (event.isShiftClick() ? 8 : 1) : (event.isShiftClick() ? -8 : -1);
                    double value = Math.max(1, reward.getAmount() + delta);
                    mutate(config -> config.set(path("amount"), value));
                });
            }
            case MONEY -> {
                setItem(slotY, createItem(Material.SUNFLOWER, "<white><bold>Moeda",
                        "<gray>Atual: <white>" + (reward.getCurrency() != null ? reward.getCurrency() : "gold"),
                        "", "<yellow>Clique pra digitar a moeda no chat"),
                        event -> promptText("crate-prompt-currency", input -> {
                    String currency = input.trim().toLowerCase(Locale.ROOT);
                    if (!plugin.getEconomyHook().isValidCurrency(currency)) {
                        player.sendMessage(plugin.getCratesMessages().parse("crate-invalid-currency"));
                        return;
                    }
                    mutate(config -> config.set(path("currency"), currency));
                }));
                setItem(slotZ, createItem(Material.GOLD_INGOT, "<white><bold>Valor",
                        "<gray>Atual: <white>" + (long) reward.getAmount(),
                        "", "<green>Esquerdo: <white>+10 (shift +500)", "<red>Direito: <white>-10 (shift -500)"), event -> {
                    double delta = event.isLeftClick() ? (event.isShiftClick() ? 500 : 10) : (event.isShiftClick() ? -500 : -10);
                    double value = Math.max(0, reward.getAmount() + delta);
                    mutate(config -> config.set(path("amount"), value));
                });
            }
            case COMMAND -> setItem(slotY, createItem(Material.COMMAND_BLOCK, "<white><bold>Comando",
                    "<gray>Atual: <white>" + (reward.getCommand() != null ? reward.getCommand() : "(nenhum)"),
                    "", "<gray>Use %player% - executado pelo console",
                    "<yellow>Clique pra digitar no chat"),
                    event -> promptText("crate-prompt-command", input ->
                            mutate(config -> config.set(path("command"), input))));
            case VIP_DAYS -> setItem(slotY, createItem(Material.NETHER_STAR, "<white><bold>Dias de VIP",
                    "<gray>Atual: <white>" + reward.getDays(),
                    "", "<green>Esquerdo: <white>+1 (shift +7)", "<red>Direito: <white>-1 (shift -7)"), event -> {
                int delta = event.isLeftClick() ? (event.isShiftClick() ? 7 : 1) : (event.isShiftClick() ? -7 : -1);
                int value = Math.max(0, reward.getDays() + delta);
                mutate(config -> config.set(path("days"), value));
            });
            case KIT -> setItem(slotY, createItem(Material.ENDER_CHEST, "<white><bold>Kit ID",
                    "<gray>Atual: <white>" + (reward.getKitId() != null ? reward.getKitId() : "(nenhum)"),
                    "", "<yellow>Clique pra digitar o ID do kit no chat"),
                    event -> promptText("crate-prompt-kit", input ->
                            mutate(config -> config.set(path("kit_id"), input.trim()))));
            case PERMISSION -> {
                setItem(slotY, createItem(Material.PAPER, "<white><bold>Permissao concedida",
                        "<gray>Atual: <white>" + (reward.getCommand() != null ? reward.getCommand() : "(nenhuma)"),
                        "", "<yellow>Clique pra digitar o node no chat"),
                        event -> promptText("crate-prompt-permission-node", input ->
                                mutate(config -> config.set(path("command"), input.trim()))));
                setItem(slotZ, createItem(Material.CLOCK, "<white><bold>Duracao",
                        "<gray>Atual: <white>" + (reward.getAmount() <= 0 ? "permanente" : (long) reward.getAmount() + "s"),
                        "", "<green>Esquerdo: <white>+60s (shift +3600s)",
                        "<red>Direito: <white>-60s (shift -3600s)", "<gray>0 = permanente"), event -> {
                    double delta = event.isLeftClick() ? (event.isShiftClick() ? 3600 : 60) : (event.isShiftClick() ? -3600 : -60);
                    double value = Math.max(0, reward.getAmount() + delta);
                    mutate(config -> config.set(path("amount"), value));
                });
            }
        }
    }

    private Reward findReward(Crate crate) {
        return crate.getRewards().stream().filter(r -> r.getId().equals(rewardId)).findFirst().orElse(null);
    }

    private String path(String field) {
        return "rewards." + rewardId + "." + field;
    }

    private void mutate(Consumer<YamlConfiguration> mutation) {
        YamlConfiguration config = plugin.getCrateFileService().load(crateId);
        mutation.accept(config);
        plugin.getCrateFileService().saveAndReload(crateId, config);
        refresh();
    }

    /** Como promptText, mas ja valida/converte pra double (aceita virgula ou ponto) antes de chamar onInput. */
    private void promptNumber(String messageKey, Consumer<Double> onInput) {
        promptText(messageKey, input -> {
            try {
                double value = Double.parseDouble(input.trim().replace(',', '.'));
                onInput.accept(value);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getCratesMessages().parse("crate-invalid-number"));
            }
        });
    }

    private void promptText(String messageKey, Consumer<String> onInput) {
        player.closeInventory();
        player.sendMessage(plugin.getCratesMessages().parse(messageKey));
        plugin.getChatInputManager().await(player.getUniqueId(), input -> {
            if (input.equalsIgnoreCase("cancelar")) {
                new RewardEditMenu(plugin, player, crateId, rewardId).open();
                return;
            }
            onInput.accept(input);
            new RewardEditMenu(plugin, player, crateId, rewardId).open();
        });
    }

    private List<String> splitPermissions(String input) {
        if (input == null || input.isBlank() || input.equalsIgnoreCase("nenhuma")) {
            return List.of();
        }
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String joinOrNone(List<String> values) {
        return values.isEmpty() ? "(nenhuma)" : String.join(", ", values);
    }

    private double roundTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Mostra ate 3 casas decimais (sem zero sobrando) - 1 casa so nao dava pra ver diferenca entre 0.001 e 0.01. */
    private String trim(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        String formatted = String.format(Locale.ROOT, "%.3f", value);
        while (formatted.endsWith("0")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        if (formatted.endsWith(".")) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }
}
