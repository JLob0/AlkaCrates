package com.alkacode.crates.crate.service;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Reward;
import com.alkacode.crates.repository.PityRepository;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

/**
 * Guaranteed reward (pity): o jogador recebe uma recompensa "garantida" a cada
 * N aberturas. As rewards marcadas com `guaranteed: true` no crate.yml sao as
 * que compoem o pool do pity.
 */
public final class PityService {

    private final AlkaCrates plugin;
    private final PityRepository pityRepository;
    private final int requiredOpens;

    public PityService(AlkaCrates plugin, PityRepository pityRepository) {
        this.plugin = plugin;
        this.pityRepository = pityRepository;
        this.requiredOpens = plugin.getConfig().getInt("pity-opens", 0);
    }

    public boolean isEnabled() {
        return requiredOpens > 0;
    }

    public void recordOpen(Player player, String crateId) {
        try {
            pityRepository.increment(player.getUniqueId(), crateId);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao registrar abertura para pity", e);
        }
    }

    /** Se o pity esta satisfeito, retorna uma reward garantida (sem consumir o contador). */
    public Reward tryClaimGuaranteed(Player player, String crateId, List<Reward> guaranteedPool) {
        if (!isEnabled() || guaranteedPool.isEmpty()) {
            return null;
        }
        try {
            int opens = pityRepository.getOpens(player.getUniqueId(), crateId);
            if (opens >= requiredOpens) {
                pityRepository.reset(player.getUniqueId(), crateId);
                return guaranteedPool.get((int) (System.currentTimeMillis() % guaranteedPool.size()));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao consultar pity", e);
        }
        return null;
    }
}
