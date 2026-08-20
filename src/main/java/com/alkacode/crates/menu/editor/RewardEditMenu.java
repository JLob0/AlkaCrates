package com.alkacode.crates.menu.editor;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.Reward;
import com.alkacode.crates.crate.model.RewardType;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Editor de uma recompensa - campos comuns + campos especificos por RewardType. */
public final class RewardEditMenu extends BaseGui {

    private final AlkaCrates plugin;
    private final String crateId;
    private final String rewardId;

    public RewardEditMenu(AlkaCrates plugin, Player player, String crateId, String rewardId) {
        super(plugin, player, "<gradient:#FFD700:#FFA500>Recompensa: " + rewardId, 6, "alkacrates-reward-edit");
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
        fillBorder(createItem(Material.BLACK_STAINED_GLASS_PANE, " "));

        setItem(10, createItem(Material.COMPASS, "<aqua><bold>Tipo",
                "<gray>Atual: <white>" + reward.getType(),
                "", "<yellow>Clique pra alternar"), event -> {
            RewardType[] values = RewardType.values();
            int next = (reward.getType().ordinal() + 1) % values.length;
            mutate(config -> config.set(path("type"), values[next].name()));
        });

        setItem(11, createItem(Material.NAME_TAG, "<yellow><bold>Nome de exibicao",
                "<gray>Atual: <white>" + (reward.getDisplayName() != null ? reward.getDisplayName() : "(nenhum)"),
                "", "<yellow>Clique pra digitar no chat"),
                event -> promptText("crate-prompt-name", input -> mutate(config -> config.set(path("display-name"), input))));

        setItem(12, createItem(Material.GOLD_NUGGET, "<gold><bold>Chance",
                "<gray>Atual: <white>" + trim(reward.getChance()),
                "", "<green>Esquerdo: <white>+1 (shift +10)", "<red>Direito: <white>-1 (shift -10)",
                "<yellow>Solte (Q): <white>digitar valor exato no chat",
                "<gray>(pra valores tipo 0.001, os cliques nao chegam la)"), event -> {
            if (event.getClick() == ClickType.DROP) {
                promptNumber("crate-prompt-chance", value -> mutate(config -> config.set(path("chance"), Math.max(0, value))));
                return;
            }
            double delta = event.isLeftClick() ? (event.isShiftClick() ? 10 : 1) : (event.isShiftClick() ? -10 : -1);
            double value = Math.max(0, reward.getChance() + delta);
            mutate(config -> config.set(path("chance"), value));
        });

        setItem(13, createItem(reward.isGuaranteed() ? Material.LIME_DYE : Material.GRAY_DYE,
                "<light_purple><bold>Garantida (pity): " + (reward.isGuaranteed() ? "SIM" : "nao"),
                "<gray>Entra no pool sorteado quando o", "<gray>pity da crate (pity-opens no config.yml) bate.",
                "", "<yellow>Clique pra alternar"),
                event -> mutate(config -> config.set(path("guaranteed"), !reward.isGuaranteed())));

        setItem(14, createItem(reward.isBroadcast() ? Material.LIME_DYE : Material.GRAY_DYE,
                "<light_purple><bold>Broadcast: " + (reward.isBroadcast() ? "SIM" : "nao"),
                "<gray>Anuncia pra todo mundo quando", "<gray>alguem ganha essa recompensa.",
                "", "<yellow>Clique pra alternar"),
                event -> mutate(config -> config.set(path("broadcast"), !reward.isBroadcast())));

        setItem(15, createItem(Material.IRON_BARS, "<red><bold>Limite por jogador",
                "<gray>Atual: <white>" + (reward.getWinLimit() < 0 ? "ilimitado" : reward.getWinLimit()),
                "", "<green>Esquerdo: <white>+1  <red>Direito: <white>-1", "<gray>-1 = ilimitado"), event -> {
            int value = reward.getWinLimit() + (event.isLeftClick() ? 1 : -1);
            mutate(config -> config.set(path("win-limit"), Math.max(-1, value)));
        });

        setItem(16, createItem(Material.CLOCK, "<red><bold>Cooldown do limite",
                "<gray>Atual: <white>" + reward.getWinLimitCooldownSeconds() + "s",
                "", "<green>Esquerdo: <white>+60s (shift +3600s)",
                "<red>Direito: <white>-60s (shift -3600s)"), event -> {
            long delta = event.isLeftClick() ? (event.isShiftClick() ? 3600 : 60) : (event.isShiftClick() ? -3600 : -60);
            long value = Math.max(0, reward.getWinLimitCooldownSeconds() + delta);
            mutate(config -> config.set(path("win-limit-cooldown"), value));
        });

        setItem(19, createItem(Material.BEDROCK, "<red><bold>Limite global",
                "<gray>Atual: <white>" + (reward.getGlobalWinLimit() < 0 ? "ilimitado" : reward.getGlobalWinLimit()),
                "", "<green>Esquerdo: <white>+1  <red>Direito: <white>-1", "<gray>-1 = ilimitado (estoque do servidor todo)"),
                event -> {
            int value = reward.getGlobalWinLimit() + (event.isLeftClick() ? 1 : -1);
            mutate(config -> config.set(path("global-win-limit"), Math.max(-1, value)));
        });

        setItem(20, createItem(Material.WRITABLE_BOOK, "<aqua><bold>Permissoes obrigatorias",
                "<gray>Atual: <white>" + joinOrNone(reward.getRequiredPermissions()),
                "", "<gray>So ganha quem tiver UMA dessas.",
                "<yellow>Clique pra digitar (separado por virgula, vazio = nenhuma)"),
                event -> promptText("crate-prompt-permissions", input -> mutate(config ->
                        config.set(path("required-permissions"), splitPermissions(input)))));

        setItem(21, createItem(Material.WRITTEN_BOOK, "<aqua><bold>Permissoes restritas",
                "<gray>Atual: <white>" + joinOrNone(reward.getRestrictedPermissions()),
                "", "<gray>Quem tiver QUALQUER uma NAO ganha.",
                "<yellow>Clique pra digitar (separado por virgula, vazio = nenhuma)"),
                event -> promptText("crate-prompt-permissions", input -> mutate(config ->
                        config.set(path("restricted-permissions"), splitPermissions(input)))));

        int attempts = plugin.getRewardPityManager().getAttempts(player.getUniqueId(), crateId, reward.getId());
        double effective = reward.hasSoftPity()
                ? Math.min(reward.getPityMaxChance(), reward.getChance() + reward.getPityIncrement() * attempts)
                : reward.getChance();
        setItem(25, createItem(Material.EXPERIENCE_BOTTLE, "<light_purple><bold>Soft pity - incremento",
                "<gray>Atual: <white>" + (reward.hasSoftPity() ? "+" + trim(reward.getPityIncrement()) + " por tentativa" : "desligado"),
                "", "<gray>A cada abertura que VOCE (o admin, pra teste)",
                "<gray>nao ganhar essa reward, a chance sobe isso.",
                "<gray>0 = desligado (fica sempre em 'Chance').",
                "", "<green>Esquerdo: <white>+0.01 (shift +1)", "<red>Direito: <white>-0.01 (shift -1)",
                "<yellow>Solte (Q): <white>digitar valor exato no chat"), event -> {
            if (event.getClick() == ClickType.DROP) {
                promptNumber("crate-prompt-pity-increment", value -> mutate(config -> config.set(path("pity-increment"), Math.max(0, value))));
                return;
            }
            double delta = event.isLeftClick() ? (event.isShiftClick() ? 1 : 0.01) : (event.isShiftClick() ? -1 : -0.01);
            double value = Math.max(0, roundTwo(reward.getPityIncrement() + delta));
            mutate(config -> config.set(path("pity-increment"), value));
        });

        setItem(26, createItem(Material.BEACON, "<light_purple><bold>Soft pity - teto",
                "<gray>Atual: <white>" + trim(reward.getPityMaxChance()),
                "<gray>Chance efetiva agora (voce): <yellow>" + trim(effective) + " <gray>(" + attempts + " tentativa(s))",
                "", "<gray>A chance nunca passa desse valor,", "<gray>mesmo com muitas tentativas acumuladas.",
                "", "<green>Esquerdo: <white>+1 (shift +10)", "<red>Direito: <white>-1 (shift -10)",
                "<yellow>Solte (Q): <white>digitar valor exato no chat"), event -> {
            if (event.getClick() == ClickType.DROP) {
                promptNumber("crate-prompt-pity-cap", value -> mutate(config -> config.set(path("pity-max-chance"), Math.max(0, value))));
                return;
            }
            double delta = event.isLeftClick() ? (event.isShiftClick() ? 10 : 1) : (event.isShiftClick() ? -10 : -1);
            double value = Math.max(0, reward.getPityMaxChance() + delta);
            mutate(config -> config.set(path("pity-max-chance"), value));
        });

        renderTypeSpecific(reward);

        setItem(31, createItem(Material.BARRIER, "<red><bold>Deletar Recompensa",
                "", "<red>Shift + botao direito pra confirmar"), event -> {
            if (event.isShiftClick() && event.isRightClick()) {
                YamlConfiguration config = plugin.getCrateFileService().load(crateId);
                plugin.getCrateFileService().removeReward(config, rewardId);
                plugin.getCrateFileService().saveAndReload(crateId, config);
                new RewardListMenu(plugin, player, crateId).open();
            }
        });

        setItem(40, createItem(Material.ARROW, "<red><bold>Voltar", "<gray>Volta pra lista de recompensas"),
                event -> new RewardListMenu(plugin, player, crateId).open());
    }

    private void renderTypeSpecific(Reward reward) {
        switch (reward.getType()) {
            case ITEM -> {
                Material icon = reward.getItem() != null ? Material.matchMaterial(reward.getItem()) : null;
                setItem(23, createItem(icon != null ? icon : Material.BARRIER, "<white><bold>Item",
                        "<gray>Atual: <white>" + reward.getItem(),
                        "", "<yellow>Clique pra digitar Material/ID no chat"),
                        event -> promptText("crate-prompt-item", input ->
                                mutate(config -> config.set(path("item"), input.trim()))));
                setItem(24, createItem(Material.HOPPER, "<white><bold>Quantidade",
                        "<gray>Atual: <white>" + (long) reward.getAmount(),
                        "", "<green>Esquerdo: <white>+1 (shift +8)", "<red>Direito: <white>-1 (shift -8)"), event -> {
                    double delta = event.isLeftClick() ? (event.isShiftClick() ? 8 : 1) : (event.isShiftClick() ? -8 : -1);
                    double value = Math.max(1, reward.getAmount() + delta);
                    mutate(config -> config.set(path("amount"), value));
                });
            }
            case MONEY -> {
                setItem(23, createItem(Material.SUNFLOWER, "<white><bold>Moeda",
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
                setItem(24, createItem(Material.GOLD_INGOT, "<white><bold>Valor",
                        "<gray>Atual: <white>" + (long) reward.getAmount(),
                        "", "<green>Esquerdo: <white>+10 (shift +500)", "<red>Direito: <white>-10 (shift -500)"), event -> {
                    double delta = event.isLeftClick() ? (event.isShiftClick() ? 500 : 10) : (event.isShiftClick() ? -500 : -10);
                    double value = Math.max(0, reward.getAmount() + delta);
                    mutate(config -> config.set(path("amount"), value));
                });
            }
            case COMMAND -> setItem(23, createItem(Material.COMMAND_BLOCK, "<white><bold>Comando",
                    "<gray>Atual: <white>" + (reward.getCommand() != null ? reward.getCommand() : "(nenhum)"),
                    "", "<gray>Use %player% - executado pelo console",
                    "<yellow>Clique pra digitar no chat"),
                    event -> promptText("crate-prompt-command", input ->
                            mutate(config -> config.set(path("command"), input))));
            case VIP_DAYS -> setItem(23, createItem(Material.NETHER_STAR, "<white><bold>Dias de VIP",
                    "<gray>Atual: <white>" + reward.getDays(),
                    "", "<green>Esquerdo: <white>+1 (shift +7)", "<red>Direito: <white>-1 (shift -7)"), event -> {
                int delta = event.isLeftClick() ? (event.isShiftClick() ? 7 : 1) : (event.isShiftClick() ? -7 : -1);
                int value = Math.max(0, reward.getDays() + delta);
                mutate(config -> config.set(path("days"), value));
            });
            case KIT -> setItem(23, createItem(Material.ENDER_CHEST, "<white><bold>Kit ID",
                    "<gray>Atual: <white>" + (reward.getKitId() != null ? reward.getKitId() : "(nenhum)"),
                    "", "<yellow>Clique pra digitar o ID do kit no chat"),
                    event -> promptText("crate-prompt-kit", input ->
                            mutate(config -> config.set(path("kit_id"), input.trim()))));
            case PERMISSION -> {
                setItem(23, createItem(Material.PAPER, "<white><bold>Permissao concedida",
                        "<gray>Atual: <white>" + (reward.getCommand() != null ? reward.getCommand() : "(nenhuma)"),
                        "", "<yellow>Clique pra digitar o node no chat"),
                        event -> promptText("crate-prompt-permission-node", input ->
                                mutate(config -> config.set(path("command"), input.trim()))));
                setItem(24, createItem(Material.CLOCK, "<white><bold>Duracao",
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
