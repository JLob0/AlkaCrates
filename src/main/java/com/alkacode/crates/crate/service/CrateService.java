package com.alkacode.crates.crate.service;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.KeyType;
import com.alkacode.crates.crate.model.Reward;
import com.alkacode.crates.crate.placement.PlacedCrate;
import com.alkacode.crates.reward.RewardDispatcher;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Logica central de abertura de crates - instantanea (sem animacao de abertura, so a
 * idle continua rodando): rola a(s) recompensa(s), entrega direto no inventario e
 * manda um resumo no chat. `all=true` consome TODAS as keys que o jogador tem daquela
 * crate de uma vez (agrupando o resumo, pra nao spammar 1 linha por unidade aberta).
 */
public final class CrateService {

    private final AlkaCrates plugin;
    private final RewardDispatcher rewardDispatcher;
    private final RewardSelector rewardSelector;
    private final RewardWinManager rewardWinManager;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private PityService pityService;

    public CrateService(AlkaCrates plugin, RewardWinManager rewardWinManager, RewardPityManager rewardPityManager) {
        this.plugin = plugin;
        this.rewardDispatcher = new RewardDispatcher(plugin);
        this.rewardWinManager = rewardWinManager;
        this.rewardSelector = new RewardSelector(rewardWinManager, rewardPityManager);
    }

    public long getCooldownSeconds() {
        return plugin.getConfig().getInt("open-cooldown", 3);
    }

    public boolean isOnCooldown(Player player) {
        Long last = cooldowns.get(player.getUniqueId());
        if (last == null) {
            return false;
        }
        return (System.currentTimeMillis() - last) < getCooldownSeconds() * 1000;
    }

    public void openCrate(Player player, PlacedCrate placedCrate, KeyType keyType, boolean all) {
        Crate crate = placedCrate.getCrate();
        if (!player.hasPermission("alkacrates.use")) {
            plugin.getCratesMessages().send(player, "crate-no-permission");
            return;
        }
        if (isOnCooldown(player)) {
            long remaining = getCooldownSeconds() - (System.currentTimeMillis() - cooldowns.getOrDefault(player.getUniqueId(), 0L)) / 1000;
            plugin.getCratesMessages().send(player, "crate-cooldown", Map.of("seconds", String.valueOf(Math.max(1, remaining))));
            return;
        }
        // cap defensivo - abrir "tudo" nao pode travar o servidor se alguem acumular
        // uma quantidade absurda de keys (evento, bug, doacao em lote, etc).
        int amount = all ? Math.min(500, plugin.getKeyService().getKeyCount(player, crate.getId(), keyType)) : 1;
        if (amount <= 0) {
            plugin.getCratesMessages().send(player, "crate-no-key", Map.of("crate", crate.getDisplayName()));
            return;
        }
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // LinkedHashMap: mantem a ordem de primeira aparicao no resumo, mais legivel que ordem aleatoria.
        Map<String, Integer> summary = new LinkedHashMap<>();
        int opened = 0;
        for (int i = 0; i < amount; i++) {
            if (!plugin.getKeyService().consumeKey(player, crate.getId(), keyType)) {
                break;
            }
            Reward reward = rollAndDeliver(player, crate);
            if (reward == null) {
                continue;
            }
            opened++;
            String label = reward.getDisplayName() != null ? reward.getDisplayName() : reward.getId();
            summary.merge(label, 1, Integer::sum);
        }
        if (opened == 0) {
            plugin.getCratesMessages().send(player, "crate-no-key", Map.of("crate", crate.getDisplayName()));
            return;
        }
        sendSummary(player, crate, opened, summary);
    }

    public void setPityService(PityService pityService) {
        this.pityService = pityService;
    }

    /** Sorteia + entrega UMA recompensa. Retorna null so se a crate nao tiver nenhuma reward configurada. */
    private Reward rollAndDeliver(Player player, Crate crate) {
        Reward reward = null;
        if (pityService != null && pityService.isEnabled()) {
            reward = pityService.tryClaimGuaranteed(player, crate.getId(), crate.getGuaranteedRewards());
            pityService.recordOpen(player, crate.getId());
        }
        if (reward == null) {
            reward = rewardSelector.select(player, crate);
        }
        if (reward == null) {
            return null;
        }
        rewardWinManager.recordWin(player, crate.getId(), reward);
        try {
            plugin.getCrateLogRepository().log(player.getUniqueId().toString(), player.getName(),
                    crate.getId(), reward.getId());
        } catch (Exception ignored) {
        }
        rewardDispatcher.execute(player, reward);
        if (reward.isBroadcast()) {
            String label = reward.getDisplayName() != null ? reward.getDisplayName() : reward.getId();
            broadcastWin(player, crate, label);
        }
        return reward;
    }

    private void sendSummary(Player player, Crate crate, int opened, Map<String, Integer> summary) {
        if (opened == 1) {
            String reward = summary.keySet().iterator().next();
            plugin.getCratesMessages().send(player, "crate-opened", Map.of(
                    "crate", crate.getDisplayName(), "reward", reward));
            return;
        }
        String rewards = summary.entrySet().stream()
                .map(e -> e.getValue() + "x " + e.getKey())
                .collect(Collectors.joining(", "));
        plugin.getCratesMessages().send(player, "crate-opened-bulk", Map.of(
                "crate", crate.getDisplayName(), "amount", String.valueOf(opened), "rewards", rewards));
    }

    /** nChat nao tem API de broadcast pra plugins - manda direto pra todos os online (ver reference-nchat-api). */
    private void broadcastWin(Player player, Crate crate, String rewardName) {
        Component message = plugin.getCratesMessages().parse("crate-broadcast", Map.of(
                "player", player.getName(),
                "crate", crate.getDisplayName(),
                "reward", rewardName));
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(message);
        }
    }

    public void cancelAllSessions() {
        plugin.getAnimationEngine().cancelAllSessions();
    }

    public RewardDispatcher getRewardDispatcher() {
        return rewardDispatcher;
    }
}
