package com.alkacode.crates.crate.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Recompensa generica de uma crate. Um so objeto cobre todos os tipos (ITEM,
 * MONEY, COMMAND, VIP_DAYS, KIT, PERMISSION) - os campos nao usados ficam null.
 */
public final class Reward {

    private final String id;
    private final RewardType type;
    private final String item;
    private final String currency;
    private final double amount;
    private final String command;
    private final String tier;
    private final int days;
    private final String kitId;
    private final double chance;
    private final String displayName;
    private final boolean guaranteed;
    private final int winLimit;
    private final long winLimitCooldownSeconds;
    private final int globalWinLimit;
    private final boolean broadcast;
    private final List<String> requiredPermissions;
    private final List<String> restrictedPermissions;
    private final double pityIncrement;
    private final double pityMaxChance;

    public Reward(String id, RewardType type, String item, String currency, double amount,
                  String command, String tier, int days, String kitId, double chance, String displayName, boolean guaranteed,
                  int winLimit, long winLimitCooldownSeconds, int globalWinLimit, boolean broadcast,
                  List<String> requiredPermissions, List<String> restrictedPermissions,
                  double pityIncrement, double pityMaxChance) {
        this.id = id;
        this.type = type;
        this.item = item;
        this.currency = currency;
        this.amount = amount;
        this.command = command;
        this.tier = tier;
        this.days = days;
        this.kitId = kitId;
        this.chance = chance;
        this.displayName = displayName;
        this.guaranteed = guaranteed;
        this.winLimit = winLimit;
        this.winLimitCooldownSeconds = winLimitCooldownSeconds;
        this.globalWinLimit = globalWinLimit;
        this.broadcast = broadcast;
        this.requiredPermissions = requiredPermissions;
        this.restrictedPermissions = restrictedPermissions;
        this.pityIncrement = pityIncrement;
        this.pityMaxChance = pityMaxChance;
    }

    public static Reward from(ConfigurationSection section) {
        String id = section.getString("id", section.getName());
        RewardType type = RewardType.valueOf(section.getString("type", "ITEM").toUpperCase());
        String item = section.getString("item");
        String currency = section.getString("currency");
        double amount = section.getDouble("amount", 0);
        String command = section.getString("command");
        String tier = section.getString("tier");
        int days = section.getInt("days", 0);
        String kitId = section.getString("kit_id");
        double chance = section.getDouble("chance", 0);
        String displayName = section.getString("display-name");
        boolean guaranteed = section.getBoolean("guaranteed", false);
        // -1 = ilimitado, em ambos. Cooldown so importa quando o limite e finito.
        int winLimit = section.getInt("win-limit", -1);
        long winLimitCooldownSeconds = section.getLong("win-limit-cooldown", 0);
        int globalWinLimit = section.getInt("global-win-limit", -1);
        boolean broadcast = section.getBoolean("broadcast", false);
        List<String> requiredPermissions = section.getStringList("required-permissions");
        List<String> restrictedPermissions = section.getStringList("restricted-permissions");
        // soft pity: 0 = desligado (chance fica sempre em "chance"). >0 = a cada tentativa
        // que essa reward NAO ganha, a chance efetiva sobe esse tanto, ate o teto abaixo.
        double pityIncrement = section.getDouble("pity-increment", 0);
        double pityMaxChance = section.getDouble("pity-max-chance", 100.0);
        return new Reward(id, type, item, currency, amount, command, tier, days, kitId, chance, displayName, guaranteed,
                winLimit, winLimitCooldownSeconds, globalWinLimit, broadcast, requiredPermissions, restrictedPermissions,
                pityIncrement, pityMaxChance);
    }

    /** Checa apenas as permissoes (required/restricted) - nao considera limites de win, isso e RewardWinManager. */
    public boolean isEligibleFor(Player player) {
        if (!restrictedPermissions.isEmpty()) {
            for (String perm : restrictedPermissions) {
                if (player.hasPermission(perm)) {
                    return false;
                }
            }
        }
        if (!requiredPermissions.isEmpty()) {
            for (String perm : requiredPermissions) {
                if (player.hasPermission(perm)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public String getId() { return id; }
    public RewardType getType() { return type; }
    public String getItem() { return item; }
    public String getCurrency() { return currency; }
    public double getAmount() { return amount; }
    public String getCommand() { return command; }
    public String getTier() { return tier; }
    public int getDays() { return days; }
    public String getKitId() { return kitId; }
    public double getChance() { return chance; }
    public String getDisplayName() { return displayName; }
    public boolean isGuaranteed() { return guaranteed; }
    public int getWinLimit() { return winLimit; }
    public long getWinLimitCooldownSeconds() { return winLimitCooldownSeconds; }
    public int getGlobalWinLimit() { return globalWinLimit; }
    public boolean isBroadcast() { return broadcast; }
    public List<String> getRequiredPermissions() { return Collections.unmodifiableList(requiredPermissions); }
    public List<String> getRestrictedPermissions() { return Collections.unmodifiableList(restrictedPermissions); }
    public double getPityIncrement() { return pityIncrement; }
    public double getPityMaxChance() { return pityMaxChance; }
    public boolean hasSoftPity() { return pityIncrement > 0; }
}
