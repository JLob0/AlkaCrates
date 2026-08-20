package com.alkacode.crates.api;

import org.bukkit.entity.Player;

/** API pública do AlkaCrates (ServicesManager) - consumida por outros plugins da rede
 * (ex: AlkaFish dando key de pesca como recompensa) sem precisar de dependência de compilação. */
public interface AlkaCratesAPI {

    /** Dá {@code amount} keys físicas da crate {@code crateId} pro jogador (cai no chão se o
     * inventário estiver lotado, mesmo comportamento do KeyService interno). No-op se o
     * crateId não existir. */
    void giveKey(Player player, String crateId, int amount);

    /** Quantas keys físicas dessa crate o jogador tem no inventário agora. */
    int getKeyCount(Player player, String crateId);

    /** true se {@code crateId} existe/está configurado (crates/*.yml). */
    boolean crateExists(String crateId);
}
