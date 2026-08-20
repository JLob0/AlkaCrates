package com.alkacode.crates.crate.service;

import com.alkacode.core.scheduler.AlkaScheduler;
import com.alkacode.crates.crate.model.Reward;
import com.alkacode.crates.repository.RewardWinRepository;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.alkacode.crates.repository.RewardWinRepository.WinRecord;
import static com.alkacode.crates.repository.RewardWinRepository.key;

/**
 * Cache de vitorias por reward - decide elegibilidade de win-limit (por jogador,
 * com cooldown de reset opcional) e global-win-limit (estoque compartilhado entre
 * todos os jogadores, ex: um jackpot unico no servidor). Cache por jogador e
 * carregado no join (write-through); cache global e carregado 1x no enable, ja
 * que o catalogo de rewards e pequeno (nao escala com jogadores).
 */
public final class RewardWinManager {

    private final RewardWinRepository repository;
    private final AlkaScheduler scheduler;
    private final Logger logger;
    private final Map<UUID, Map<String, WinRecord>> playerCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> globalCache = new ConcurrentHashMap<>();

    public RewardWinManager(RewardWinRepository repository, AlkaScheduler scheduler, Logger logger) {
        this.repository = repository;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    /** Carga unica e sincrona no onEnable - mesmo precedente do createTable() dos outros repositorios. */
    public void loadGlobalOnEnable() {
        try {
            repository.loadAllGlobal().forEach((k, wins) -> globalCache.put(k, new AtomicInteger(wins)));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Falha ao carregar contadores globais de reward", e);
        }
    }

    public void onJoin(UUID uuid) {
        scheduler.runAsync(() -> {
            try {
                Map<String, WinRecord> loaded = repository.loadAll(uuid);
                playerCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).putAll(loaded);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Falha ao carregar vitorias de reward de " + uuid, e);
            }
        });
    }

    public void onQuit(UUID uuid) {
        playerCache.remove(uuid);
    }

    /** True se a reward ainda pode ser sorteada para esse jogador (permissoes + win-limit + global-win-limit). */
    public boolean isEligible(Player player, String crateId, Reward reward) {
        if (!reward.isEligibleFor(player)) {
            return false;
        }
        if (reward.getGlobalWinLimit() >= 0) {
            AtomicInteger global = globalCache.get(key(crateId, reward.getId()));
            if (global != null && global.get() >= reward.getGlobalWinLimit()) {
                return false;
            }
        }
        if (reward.getWinLimit() < 0) {
            return true;
        }
        Map<String, WinRecord> playerWins = playerCache.get(player.getUniqueId());
        if (playerWins == null) {
            return true;
        }
        WinRecord record = playerWins.getOrDefault(key(crateId, reward.getId()), WinRecord.EMPTY);
        if (record.wins() < reward.getWinLimit()) {
            return true;
        }
        if (reward.getWinLimitCooldownSeconds() <= 0) {
            return false;
        }
        long elapsed = (System.currentTimeMillis() / 1000L) - record.lastWinEpochSeconds();
        return elapsed >= reward.getWinLimitCooldownSeconds();
    }

    /** Registra que o jogador ganhou a reward - atualiza cache na hora e persiste de forma assincrona. */
    public void recordWin(Player player, String crateId, Reward reward) {
        long now = System.currentTimeMillis() / 1000L;
        String k = key(crateId, reward.getId());
        UUID uuid = player.getUniqueId();

        playerCache.computeIfAbsent(uuid, id -> new ConcurrentHashMap<>())
                .merge(k, new WinRecord(1, now), (old, fresh) -> new WinRecord(old.wins() + 1, now));
        globalCache.computeIfAbsent(k, id -> new AtomicInteger()).incrementAndGet();

        scheduler.runAsync(() -> {
            try {
                repository.incrementPlayer(uuid, crateId, reward.getId(), now);
                repository.incrementGlobal(crateId, reward.getId());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Falha ao persistir vitoria de reward de " + uuid, e);
            }
        });
    }
}
