package com.alkacode.crates.crate.model;

import org.bukkit.configuration.ConfigurationSection;

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

    public Reward(String id, RewardType type, String item, String currency, double amount,
                  String command, String tier, int days, String kitId, double chance, String displayName, boolean guaranteed) {
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
        return new Reward(id, type, item, currency, amount, command, tier, days, kitId, chance, displayName, guaranteed);
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
}
