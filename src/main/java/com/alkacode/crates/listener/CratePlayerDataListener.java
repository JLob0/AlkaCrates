package com.alkacode.crates.listener;

import com.alkacode.crates.crate.service.PityManager;
import com.alkacode.crates.crate.service.RewardPityManager;
import com.alkacode.crates.crate.service.RewardWinManager;
import com.alkacode.crates.crate.service.VirtualKeyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Carrega/descarrega os caches por jogador dos managers de keys/pity/reward-wins (ver R7). */
public final class CratePlayerDataListener implements Listener {

    private final VirtualKeyManager virtualKeyManager;
    private final PityManager pityManager;
    private final RewardWinManager rewardWinManager;
    private final RewardPityManager rewardPityManager;

    public CratePlayerDataListener(VirtualKeyManager virtualKeyManager, PityManager pityManager,
                                    RewardWinManager rewardWinManager, RewardPityManager rewardPityManager) {
        this.virtualKeyManager = virtualKeyManager;
        this.pityManager = pityManager;
        this.rewardWinManager = rewardWinManager;
        this.rewardPityManager = rewardPityManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        virtualKeyManager.onJoin(event.getPlayer().getUniqueId());
        pityManager.onJoin(event.getPlayer().getUniqueId());
        rewardWinManager.onJoin(event.getPlayer().getUniqueId());
        rewardPityManager.onJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        virtualKeyManager.onQuit(event.getPlayer().getUniqueId());
        pityManager.onQuit(event.getPlayer().getUniqueId());
        rewardWinManager.onQuit(event.getPlayer().getUniqueId());
        rewardPityManager.onQuit(event.getPlayer().getUniqueId());
    }
}
