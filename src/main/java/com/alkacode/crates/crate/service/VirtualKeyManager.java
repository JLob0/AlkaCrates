package com.alkacode.crates.crate.service;

import com.alkacode.core.scheduler.AlkaScheduler;
import com.alkacode.crates.repository.VirtualKeyRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache em memoria das keys virtuais por jogador (write-through), carregado inteiro no
 * join (ver AlkaCrates listener de join/quit) para nunca bater no banco de forma sincrona
 * durante a interacao do jogador (R1-R7 do AlkaCore: zero query sincrona na main thread).
 */
public final class VirtualKeyManager {

    private final VirtualKeyRepository repository;
    private final AlkaScheduler scheduler;
    private final Logger logger;
    private final Map<UUID, Map<String, AtomicInteger>> cache = new ConcurrentHashMap<>();

    public VirtualKeyManager(VirtualKeyRepository repository, AlkaScheduler scheduler, Logger logger) {
        this.repository = repository;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    public void onJoin(UUID uuid) {
        scheduler.runAsync(() -> {
            try {
                Map<String, Integer> loaded = repository.loadAll(uuid);
                Map<String, AtomicInteger> playerCache = cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
                loaded.forEach((crateId, amount) -> playerCache.putIfAbsent(crateId, new AtomicInteger(amount)));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Falha ao carregar keys virtuais de " + uuid, e);
            }
        });
    }

    public void onQuit(UUID uuid) {
        cache.remove(uuid);
    }

    /** false enquanto o load assincrono do join ainda nao terminou. */
    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public int getKeys(UUID uuid, String crateId) {
        Map<String, AtomicInteger> playerCache = cache.get(uuid);
        if (playerCache == null) {
            return 0;
        }
        AtomicInteger amount = playerCache.get(crateId);
        return amount != null ? amount.get() : 0;
    }

    /** Adiciona keys ao cache na hora e agenda a persistencia assincrona. */
    public void addKeys(UUID uuid, String crateId, int amount) {
        cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(crateId, k -> new AtomicInteger())
                .addAndGet(amount);
        scheduler.runAsync(() -> {
            try {
                repository.addKeys(uuid, crateId, amount);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Falha ao persistir keys virtuais de " + uuid, e);
            }
        });
    }

    /** Remove ate `amount` (nunca mais do que o saldo tem) - usado pelo saque da mochila. Retorna quantas de fato saiu. */
    public int removeKeys(UUID uuid, String crateId, int amount) {
        Map<String, AtomicInteger> playerCache = cache.get(uuid);
        if (playerCache == null || amount <= 0) {
            return 0;
        }
        AtomicInteger counter = playerCache.get(crateId);
        if (counter == null) {
            return 0;
        }
        int removed;
        int current;
        do {
            current = counter.get();
            removed = Math.min(current, amount);
            if (removed <= 0) {
                return 0;
            }
        } while (!counter.compareAndSet(current, current - removed));

        int finalRemoved = removed;
        scheduler.runAsync(() -> {
            try {
                repository.removeKeys(uuid, crateId, finalRemoved);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Falha ao persistir remocao de keys virtuais de " + uuid, e);
            }
        });
        return removed;
    }

    /** Consome 1 key se houver saldo (CAS - seguro contra corrida entre threads). Retorna true se conseguiu. */
    public boolean consumeKey(UUID uuid, String crateId) {
        Map<String, AtomicInteger> playerCache = cache.get(uuid);
        if (playerCache == null) {
            return false;
        }
        AtomicInteger amount = playerCache.get(crateId);
        if (amount == null) {
            return false;
        }
        int current;
        do {
            current = amount.get();
            if (current <= 0) {
                return false;
            }
        } while (!amount.compareAndSet(current, current - 1));

        scheduler.runAsync(() -> {
            try {
                repository.removeKeys(uuid, crateId, 1);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Falha ao persistir consumo de key virtual de " + uuid, e);
            }
        });
        return true;
    }
}
