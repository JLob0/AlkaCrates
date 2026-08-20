package com.alkacode.crates.crate.service;

import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.Reward;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Filtra o pool de rewards de uma crate pelas regras de elegibilidade do jogador
 * (permissoes, win-limit, global-win-limit - ver RewardWinManager) e sorteia por
 * peso dentro do que sobrou. Se o filtro zerar o pool (ex: todo mundo bateu o
 * limite de todas as rewards ao mesmo tempo), cai para o pool completo em vez de
 * nao entregar nada - nunca deixamos o jogador de mao vazia por causa de um limite.
 *
 * O sorteio usa a chance EFETIVA de cada reward (base + soft pity acumulado - ver
 * RewardPityManager), nao o `chance` estatico do YAML.
 */
public final class RewardSelector {

    private final RewardWinManager winManager;
    private final RewardPityManager pityManager;

    public RewardSelector(RewardWinManager winManager, RewardPityManager pityManager) {
        this.winManager = winManager;
        this.pityManager = pityManager;
    }

    public Reward select(Player player, Crate crate) {
        List<Reward> rewards = crate.getRewards();
        List<Reward> eligible = rewards.stream()
                .filter(r -> winManager.isEligible(player, crate.getId(), r))
                .collect(Collectors.toList());
        List<Reward> pool = eligible.isEmpty() ? rewards : eligible;

        Reward winner = rollWithPity(player, crate.getId(), pool);
        if (winner != null) {
            pityManager.recordRoll(player, crate.getId(), pool, winner);
        }
        return winner;
    }

    private Reward rollWithPity(Player player, String crateId, List<Reward> pool) {
        if (pool.isEmpty()) {
            return null;
        }
        double total = 0;
        double[] chances = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            chances[i] = pityManager.effectiveChance(player, crateId, pool.get(i));
            total += chances[i];
        }
        if (total <= 0) {
            return pool.get(0);
        }
        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cumulative = 0;
        for (int i = 0; i < pool.size(); i++) {
            cumulative += chances[i];
            if (roll <= cumulative) {
                return pool.get(i);
            }
        }
        return pool.get(pool.size() - 1);
    }
}
