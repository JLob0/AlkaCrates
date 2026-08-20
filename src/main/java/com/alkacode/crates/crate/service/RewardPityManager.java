package com.alkacode.crates.crate.service;

import com.alkacode.core.scheduler.AlkaScheduler;
import com.alkacode.crates.crate.model.Reward;
import com.alkacode.crates.repository.RewardPityRepository;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.alkacode.crates.repository.RewardPityRepository.key;

/**
 * Soft pity: cache de "tentativas sem ganhar" por jogador/crate/reward, usado pra
 * calcular a chance EFETIVA de uma reward com `pity-increment > 0` (ver RewardSelector).
 * Nao confundir com PityManager (esse ali e o pity "duro" - contador de aberturas da
 * crate inteira que libera um pool `guaranteed` de uma vez).
 */
public final class RewardPityManager {

    private final RewardPityRepository repository;
    private final AlkaScheduler scheduler;
    private final Logger logger;
    private final Map<UUID, Map<String, AtomicInteger>> cache = new ConcurrentHashMap<>();

    public RewardPityManager(RewardPityRepository repository, AlkaScheduler scheduler, Logger logger) {
        this.repository = repository;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    public void onJoin(UUID uuid) {
        scheduler.runAsync(() -> {
            try {
                Map<String, Integer> loaded = repository.loadAll(uuid);
                Map<String, AtomicInteger> playerCache = cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
                loaded.forEach((k, attempts) -> playerCache.putIfAbsent(k, new AtomicInteger(attempts)));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Falha ao carregar soft pity de " + uuid, e);
            }
        });
    }

    public void onQuit(UUID uuid) {
        cache.remove(uuid);
    }

    public int getAttempts(UUID uuid, String crateId, String rewardId) {
        Map<String, AtomicInteger> playerCache = cache.get(uuid);
        if (playerCache == null) {
            return 0;
        }
        AtomicInteger attempts = playerCache.get(key(crateId, rewardId));
        return attempts != null ? attempts.get() : 0;
    }

    /** Chance efetiva (base + incremento*tentativas, limitada ao teto) pra uma reward com soft pity ligado. */
    public double effectiveChance(Player player, String crateId, Reward reward) {
        if (!reward.hasSoftPity()) {
            return reward.getChance();
        }
        int attempts = getAttempts(player.getUniqueId(), crateId, reward.getId());
        double boosted = reward.getChance() + reward.getPityIncrement() * attempts;
        return Math.min(reward.getPityMaxChance(), boosted);
    }

    /**
     * Chamado apos cada sorteio: toda reward do pool com soft pity ligado que NAO ganhou
     * tem sua tentativa incrementada; a vencedora (se tiver soft pity) e resetada pra 0.
     */
    public void recordRoll(Player player, String crateId, List<Reward> pool, Reward winner) {
        UUID uuid = player.getUniqueId();
        for (Reward reward : pool) {
            if (!reward.hasSoftPity()) {
                continue;
            }
            String k = key(crateId, reward.getId());
            if (reward.equals(winner)) {
                Map<String, AtomicInteger> playerCache = cache.get(uuid);
                if (playerCache != null) {
                    AtomicInteger counter = playerCache.get(k);
                    if (counter != null) {
                        counter.set(0);
                    }
                }
                scheduler.runAsync(() -> {
                    try {
                        repository.reset(uuid, crateId, reward.getId());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Falha ao resetar soft pity de " + uuid, e);
                    }
                });
            } else {
                cache.computeIfAbsent(uuid, id -> new ConcurrentHashMap<>())
                        .computeIfAbsent(k, id -> new AtomicInteger())
                        .incrementAndGet();
                scheduler.runAsync(() -> {
                    try {
                        repository.increment(uuid, crateId, reward.getId());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Falha ao persistir soft pity de " + uuid, e);
                    }
                });
            }
        }
    }
}
