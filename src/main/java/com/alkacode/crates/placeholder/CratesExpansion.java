package com.alkacode.crates.placeholder;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.KeyType;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Expansao de placeholders do AlkaCrates: %alkacrates_keys_<crate>%, %alkacrates_total_keys%. */
public final class CratesExpansion extends PlaceholderExpansion {

    private final AlkaCrates plugin;

    public CratesExpansion(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "alkacrates";
    }

    @Override
    public @NotNull String getAuthor() {
        return "AlkaCode";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "0";
        }
        if (params.startsWith("keys_")) {
            String crateId = params.substring("keys_".length());
            return String.valueOf(plugin.getKeyService().getKeyCount(player, crateId, KeyType.VIRTUAL)
                    + plugin.getKeyService().getKeyCount(player, crateId, KeyType.PHYSICAL));
        }
        if (params.equals("total_keys")) {
            int total = 0;
            for (Crate crate : plugin.getCratesConfig().getCrates()) {
                total += plugin.getKeyService().getKeyCount(player, crate.getId(), KeyType.VIRTUAL)
                        + plugin.getKeyService().getKeyCount(player, crate.getId(), KeyType.PHYSICAL);
            }
            return String.valueOf(total);
        }
        if (params.startsWith("crate_name_")) {
            String crateId = params.substring("crate_name_".length());
            Crate crate = plugin.getCratesConfig().getCrate(crateId);
            return crate != null ? crate.getDisplayName() : "";
        }
        return null;
    }
}
