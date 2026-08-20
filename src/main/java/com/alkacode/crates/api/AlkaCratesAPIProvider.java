package com.alkacode.crates.api;

import com.alkacode.crates.config.CratesConfig;
import com.alkacode.crates.crate.model.KeyType;
import com.alkacode.crates.crate.service.KeyService;
import org.bukkit.entity.Player;

public final class AlkaCratesAPIProvider implements AlkaCratesAPI {

    private final KeyService keyService;
    private final CratesConfig cratesConfig;

    public AlkaCratesAPIProvider(KeyService keyService, CratesConfig cratesConfig) {
        this.keyService = keyService;
        this.cratesConfig = cratesConfig;
    }

    @Override
    public void giveKey(Player player, String crateId, int amount) {
        if (!crateExists(crateId) || amount <= 0) return;
        keyService.giveKey(player, crateId, amount, KeyType.PHYSICAL);
    }

    @Override
    public int getKeyCount(Player player, String crateId) {
        return keyService.getKeyCount(player, crateId, KeyType.PHYSICAL);
    }

    @Override
    public boolean crateExists(String crateId) {
        return cratesConfig.getCrate(crateId) != null;
    }
}
