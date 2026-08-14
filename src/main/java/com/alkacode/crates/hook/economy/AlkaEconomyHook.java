package com.alkacode.crates.hook.economy;

import com.alkacode.economy.AlkaEconomyPlugin;
import com.alkacode.economy.EconomyManager;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Hook para a economia principal da network (AlkaEconomy). Como AlkaEconomy e
 * hard depend no plugin.yml, usamos a API publica direto: pega a instancia do
 * AlkaEconomyPlugin e usa o {@link EconomyManager}. Suporta todas as moedas
 * dinamicas (coins, drakonio, nacar, escarion, souls, e qualquer uma criada).
 */
public final class AlkaEconomyHook {

    private final EconomyManager economyManager;
    private final boolean available;

    public AlkaEconomyHook(Server server) {
        Plugin plugin = server.getPluginManager().getPlugin("AlkaEconomy");
        if (plugin instanceof AlkaEconomyPlugin alkaEconomyPlugin) {
            this.economyManager = alkaEconomyPlugin.getEconomyManager();
            this.available = true;
        } else {
            this.economyManager = null;
            this.available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isValidCurrency(String currency) {
        return available && economyManager.isValidCurrency(currency);
    }

    public double getBalance(UUID uuid, String currency) {
        return available ? economyManager.getBalance(uuid, currency) : 0;
    }

    public boolean has(UUID uuid, String currency, double amount) {
        return available && economyManager.has(uuid, currency, amount);
    }

    public void deposit(UUID uuid, String currency, double amount) {
        if (available) {
            economyManager.addBalance(uuid, currency, amount);
        }
    }

    public boolean withdraw(UUID uuid, String currency, double amount) {
        if (!available || !has(uuid, currency, amount)) {
            return false;
        }
        economyManager.removeBalance(uuid, currency, amount);
        return true;
    }

    public String format(double amount) {
        return EconomyManager.formatValue(amount);
    }
}
