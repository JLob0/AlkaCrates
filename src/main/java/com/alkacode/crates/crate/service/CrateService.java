package com.alkacode.crates.crate.service;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.CrateSession;
import com.alkacode.crates.crate.model.KeyType;
import com.alkacode.crates.crate.model.Reward;
import com.alkacode.crates.crate.placement.PlacedCrate;
import com.alkacode.crates.reward.RewardDispatcher;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Logica central de abertura de crates. */
public final class CrateService {

    private final AlkaCrates plugin;
    private final RewardDispatcher rewardDispatcher;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, CrateSession> activeSessions = new ConcurrentHashMap<>();
    private PityService pityService;

    public CrateService(AlkaCrates plugin) {
        this.plugin = plugin;
        this.rewardDispatcher = new RewardDispatcher(plugin);
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

    public void openCrate(Player player, PlacedCrate placedCrate, KeyType keyType) {
        Crate crate = placedCrate.getCrate();
        if (activeSessions.containsKey(player.getUniqueId())) {
            plugin.getCratesMessages().send(player, "crate-already-open");
            return;
        }
        if (isOnCooldown(player)) {
            long remaining = getCooldownSeconds() - (System.currentTimeMillis() - cooldowns.getOrDefault(player.getUniqueId(), 0L)) / 1000;
            plugin.getCratesMessages().send(player, "crate-cooldown", Map.of("seconds", String.valueOf(Math.max(1, remaining))));
            return;
        }
        if (!plugin.getKeyService().consumeKey(player, crate.getId(), keyType)) {
            plugin.getCratesMessages().send(player, "crate-no-key", Map.of("crate", crate.getDisplayName()));
            return;
        }
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        CrateSession session = new CrateSession(player, crate, placedCrate);
        activeSessions.put(player.getUniqueId(), session);

        plugin.getAnimationEngine().playOpening(session, () -> deliverReward(player, session, placedCrate));
    }

    public void setPityService(PityService pityService) {
        this.pityService = pityService;
    }

    private void deliverReward(Player player, CrateSession session, PlacedCrate placedCrate) {
        activeSessions.remove(player.getUniqueId());
        Reward reward = null;
        if (pityService != null && pityService.isEnabled()) {
            reward = pityService.tryClaimGuaranteed(player, session.getCrate().getId(),
                    session.getCrate().getGuaranteedRewards());
            pityService.recordOpen(player, session.getCrate().getId());
        }
        if (reward == null) {
            reward = session.getCrate().rollReward();
        }
        if (reward == null) {
            return;
        }
        // log de abertura
        try {
            plugin.getCrateLogRepository().log(player.getUniqueId().toString(), player.getName(),
                    session.getCrate().getId(), reward.getId());
        } catch (Exception ignored) {
        }
        // mostra o item sorteado no display por 60 ticks antes de restaurar
        CrateDisplayBridge.showReward(placedCrate, reward, rewardDispatcher);
        rewardDispatcher.execute(player, reward);
        String rewardName = reward.getDisplayName() != null ? reward.getDisplayName() : reward.getId();
        plugin.getCratesMessages().send(player, "crate-opened", Map.of(
                "crate", session.getCrate().getDisplayName(),
                "reward", rewardName));
    }

    public void cancelAllSessions() {
        plugin.getAnimationEngine().cancelAllSessions();
        activeSessions.clear();
    }

    public RewardDispatcher getRewardDispatcher() {
        return rewardDispatcher;
    }

    /** Ponte para nao acoplar CrateService a CrateDisplay internamente. */
    private static final class CrateDisplayBridge {
        static void showReward(PlacedCrate placedCrate, Reward reward, RewardDispatcher dispatcher) {
            if (placedCrate == null || placedCrate.getDisplay() == null) {
                return;
            }
            ItemStack original = placedCrate.getDisplay().getItemDisplay().getItemStack();
            ItemStack display = dispatcher.resolveDisplayItem(reward);
            if (display == null) {
                return;
            }
            placedCrate.getDisplay().showReward(display, 60, original);
        }
    }
}
