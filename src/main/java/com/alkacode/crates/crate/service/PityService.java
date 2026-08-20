package com.alkacode.crates.crate.service;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.Reward;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Guaranteed reward (pity): o jogador recebe uma recompensa "garantida" a cada
 * N aberturas. As rewards marcadas com `guaranteed: true` no crate.yml sao as
 * que compoem o pool do pity (sorteadas por peso entre si, nao uniformemente).
 */
public final class PityService {

    private final PityManager pityManager;
    private final int requiredOpens;

    public PityService(AlkaCrates plugin, PityManager pityManager) {
        this.pityManager = pityManager;
        this.requiredOpens = plugin.getConfig().getInt("pity-opens", 0);
    }

    public boolean isEnabled() {
        return requiredOpens > 0;
    }

    public int getRequiredOpens() {
        return requiredOpens;
    }

    /** Aberturas acumuladas da crate pra esse jogador (contador do pity "duro" - ver PityManager). */
    public int getOpens(Player player, String crateId) {
        return pityManager.getOpens(player.getUniqueId(), crateId);
    }

    public void recordOpen(Player player, String crateId) {
        pityManager.increment(player.getUniqueId(), crateId);
    }

    /** Se o pity esta satisfeito, retorna uma reward garantida (sorteada por peso) e zera o contador. */
    public Reward tryClaimGuaranteed(Player player, String crateId, List<Reward> guaranteedPool) {
        if (!isEnabled() || guaranteedPool.isEmpty()) {
            return null;
        }
        int opens = pityManager.getOpens(player.getUniqueId(), crateId);
        if (opens < requiredOpens) {
            return null;
        }
        pityManager.reset(player.getUniqueId(), crateId);
        return Crate.rollFrom(guaranteedPool);
    }
}
