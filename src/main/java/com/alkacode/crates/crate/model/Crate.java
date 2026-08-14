package com.alkacode.crates.crate.model;

import com.alkacode.crates.animation.IdleAnimation;
import com.alkacode.crates.animation.OpeningSequence;
import com.alkacode.crates.engine.CrateEngineType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Config de uma crate: display, preco, modo, animacao e engine. */
public final class Crate {

    private final String id;
    private final String displayName;
    private final CrateEngineType engineType;
    private final String vanillaItem;
    private final double[] scale;
    private final double price;
    private final String priceCurrency;
    private final IdleAnimation idleAnimation;
    private final OpeningSequence openingSequence;
    private final List<Reward> rewards;
    private final List<Reward> guaranteedRewards;

    public Crate(String id, String displayName, CrateEngineType engineType, String vanillaItem,
                 double[] scale, double price, String priceCurrency,
                 IdleAnimation idleAnimation, OpeningSequence openingSequence, List<Reward> rewards) {
        this.id = id;
        this.displayName = displayName;
        this.engineType = engineType;
        this.vanillaItem = vanillaItem;
        this.scale = scale;
        this.price = price;
        this.priceCurrency = priceCurrency;
        this.idleAnimation = idleAnimation;
        this.openingSequence = openingSequence;
        this.rewards = rewards;
        this.guaranteedRewards = rewards.stream().filter(Reward::isGuaranteed).toList();
    }

    public static Crate from(ConfigurationSection section, IdleAnimation defaultIdle, OpeningSequence defaultOpening) {
        String id = section.getString("id", section.getName());
        String displayName = section.getString("display.name", id);
        CrateEngineType engineType;
        try {
            engineType = CrateEngineType.valueOf(section.getString("display.engine", "VANILLA").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            engineType = CrateEngineType.VANILLA;
        }
        String vanillaItem = section.getString("display.vanilla.item", "DIAMOND_BLOCK");
        double[] scale = section.getDoubleList("display.vanilla.scale").stream()
                .mapToDouble(Double::doubleValue).toArray();
        if (scale.length < 3) {
            scale = new double[]{0.8, 0.8, 0.8};
        }
        double price = section.getDouble("price.amount", 0);
        String priceCurrency = section.getString("price.currency", "coins");

        ConfigurationSection anim = section.getConfigurationSection("animation");
        IdleAnimation idle = defaultIdle;
        OpeningSequence opening = defaultOpening;
        if (anim != null) {
            if (anim.getConfigurationSection("idle") != null) {
                idle = IdleAnimation.from(anim.getConfigurationSection("idle"));
            }
            if (anim.getConfigurationSection("opening") != null) {
                opening = OpeningSequence.from(anim.getConfigurationSection("opening"));
            }
        }

        List<Reward> rewards = new ArrayList<>();
        ConfigurationSection rewardsSection = section.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                rewards.add(Reward.from(rewardsSection.getConfigurationSection(key)));
            }
        }
        return new Crate(id, displayName, engineType, vanillaItem, scale, price, priceCurrency,
                idle, opening, rewards);
    }

    /** Sorteia uma recompensa por chance acumulada. */
    public Reward rollReward() {
        double total = 0;
        for (Reward reward : rewards) {
            total += reward.getChance();
        }
        if (total <= 0) {
            return rewards.isEmpty() ? null : rewards.get(0);
        }
        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cumulative = 0;
        for (Reward reward : rewards) {
            cumulative += reward.getChance();
            if (roll <= cumulative) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public CrateEngineType getEngineType() { return engineType; }
    public String getVanillaItem() { return vanillaItem; }
    public double[] getScale() { return scale; }
    public double getPrice() { return price; }
    public String getPriceCurrency() { return priceCurrency; }
    public IdleAnimation getIdleAnimation() { return idleAnimation; }
    public OpeningSequence getOpeningSequence() { return openingSequence; }
    public List<Reward> getRewards() { return rewards; }
    public List<Reward> getGuaranteedRewards() { return guaranteedRewards; }
}
