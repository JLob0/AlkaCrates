package com.alkacode.crates.crate.service;

import com.alkacode.core.scheduler.AlkaScheduler;
import com.alkacode.crates.repository.PityRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache em memoria do contador de aberturas por jogador/crate usado pelo pity
 * (guaranteed reward), carregado no join e persistido de forma assincrona
 * (mesma logica de write-through do VirtualKeyManager - ver R7 do AlkaCore).
 */
public final class PityManager {

    private final PityRepository repository;
    private final AlkaScheduler scheduler;
    private final Logger logger;
    private final Map<UUID, Map<String, AtomicInteger>> cache = new ConcurrentHashMap<>();

    public PityManager(PityRepository repository, AlkaScheduler scheduler, Logger logger) {
        this.repository = repository;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    public void onJoin(UUID uuid) {
        scheduler.runAsync(() -> {
            try {
                Map<String, Integer> loaded = repository.loadAll(uuid);
                Map<String, AtomicInteger> playerCache = cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
                loaded.forEach((crateId, opens) -> playerCache.putIfAbsent(crateId, new AtomicInteger(opens)));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Falha ao carregar pity de " + uuid, e);
            }
        });
    }

    public void onQuit(UUID uuid) {
        cache.remove(uuid);
    }

    public int getOpens(UUID uuid, String crateId) {
        Map<String, AtomicInteger> playerCache = cache.get(uuid);
        if (playerCache == null) {
            return 0;
        }
        AtomicInteger opens = playerCache.get(crateId);
        return opens != null ? opens.get() : 0;
    }

    public void increment(UUID uuid, String crateId) {
        cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(crateId, k -> new AtomicInteger())
                .incrementAndGet();
        scheduler.runAsync(() -> {
            try {
                repository.increment(uuid, crateId);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Falha ao persistir pity de " + uuid, e);
            }
        });
    }

    public void reset(UUID uuid, String crateId) {
        Map<String, AtomicInteger> playerCache = cache.get(uuid);
        if (playerCache != null) {
            AtomicInteger opens = playerCache.get(crateId);
            if (opens != null) {
                opens.set(0);
            }
        }
        scheduler.runAsync(() -> {
            try {
                repository.reset(uuid, crateId);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Falha ao persistir reset de pity de " + uuid, e);
            }
        });
    }
}
