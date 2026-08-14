package com.alkacode.crates.reward;

import com.alkacode.crates.crate.model.Reward;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Executa uma recompensa de acordo com o tipo. */
public interface RewardExecutor {

    /** Executa a recompensa no jogador. */
    void execute(Player player, Reward reward);

    /** Resolve o ItemStack de exibicao da recompensa (para preview/reveal). Null se nao aplicavel. */
    default ItemStack resolveDisplayItem(Reward reward) {
        return null;
    }
}
