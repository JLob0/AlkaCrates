package com.alkacode.crates.reward;

import com.alkacode.crates.crate.model.Reward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Recompensa VIP por dias. Integra com o AlkaVips via reflection best-effort:
 * tenta achar um metodo de ativacao/credito na API publica. Se a API nao expor
 * nada usavel, loga aviso e ignora (nao quebra a abertura).
 */
public final class VipReward implements RewardExecutor {

    @Override
    public void execute(Player player, Reward reward) {
        Plugin vips = Bukkit.getPluginManager().getPlugin("AlkaVips");
        if (vips == null) {
            return;
        }
        try {
            Object api = resolveApi(vips);
            if (api == null) {
                return;
            }
            // try addCredits(player, days * someValue) como fallback generico
            try {
                Method addCredits = api.getClass().getMethod("addCredits", java.util.UUID.class, double.class);
                addCredits.invoke(api, player.getUniqueId(), (double) reward.getDays());
            } catch (NoSuchMethodException ignored) {
                vips.getLogger().warning("[AlkaCrates] AlkaVipsAPI nao expoe addCredits - reward VIP_DAYS ignorado.");
            }
        } catch (Throwable t) {
            vips.getLogger().log(Level.WARNING, "[AlkaCrates] Falha ao processar reward VIP_DAYS", t);
        }
    }

    private Object resolveApi(Plugin vips) {
        try {
            // procura AlkaVipsAPIProvider instanciado no plugin
            for (java.lang.reflect.Field field : vips.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(vips);
                if (value != null && value.getClass().getSimpleName().contains("AlkaVipsAPI")) {
                    return value;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
