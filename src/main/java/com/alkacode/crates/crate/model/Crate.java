package com.alkacode.crates.crate.model;

import com.alkacode.crates.animation.IdleAnimation;
import com.alkacode.crates.animation.OpeningSequence;
import com.alkacode.crates.engine.CrateEngineType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Config de uma crate: display, preco, modo, animacao e engine. */
public final class Crate {

    private final String id;
    private final String displayName;
    private final List<String> displayLore;
    private final CrateEngineType engineType;
    private final String vanillaItem;
    private final ItemStack customDisplayItem;
    private final String blockMaterial;
    private final double[] scale;
    private final double price;
    private final String priceCurrency;
    private final String keyMaterial;
    private final String keyName;
    private final List<String> keyLore;
    private final IdleAnimation idleAnimation;
    private final OpeningSequence openingSequence;
    private final List<Reward> rewards;
    private final List<Reward> guaranteedRewards;

    public Crate(String id, String displayName, List<String> displayLore, CrateEngineType engineType, String vanillaItem,
                 ItemStack customDisplayItem, String blockMaterial,
                 double[] scale, double price, String priceCurrency,
                 String keyMaterial, String keyName, List<String> keyLore,
                 IdleAnimation idleAnimation, OpeningSequence openingSequence, List<Reward> rewards) {
        this.id = id;
        this.displayName = displayName;
        this.displayLore = displayLore;
        this.engineType = engineType;
        this.vanillaItem = vanillaItem;
        this.customDisplayItem = customDisplayItem;
        this.blockMaterial = blockMaterial;
        this.scale = scale;
        this.price = price;
        this.priceCurrency = priceCurrency;
        this.keyMaterial = keyMaterial;
        this.keyName = keyName;
        this.keyLore = keyLore;
        this.idleAnimation = idleAnimation;
        this.openingSequence = openingSequence;
        this.rewards = rewards;
        this.guaranteedRewards = rewards.stream().filter(Reward::isGuaranteed).toList();
    }

    public static Crate from(ConfigurationSection section, IdleAnimation defaultIdle, OpeningSequence defaultOpening) {
        String id = section.getString("id", section.getName());
        String displayName = section.getString("display.name", id);
        List<String> displayLore = section.getStringList("display.lore");
        CrateEngineType engineType;
        try {
            engineType = CrateEngineType.valueOf(section.getString("display.engine", "VANILLA").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            engineType = CrateEngineType.VANILLA;
        }
        String vanillaItem = section.getString("display.vanilla.item", "DIAMOND_BLOCK");
        // item arrastado no editor (qualquer item, inclusive custom com NBT/ItemsAdder/etc) -
        // ItemStack serializado nativamente pelo Bukkit, tem prioridade sobre display.vanilla.item.
        ItemStack customDisplayItem = section.isItemStack("display.custom-item")
                ? section.getItemStack("display.custom-item") : null;
        String blockMaterial = section.getString("display.block", "CHEST");
        double[] scale = section.getDoubleList("display.vanilla.scale").stream()
                .mapToDouble(Double::doubleValue).toArray();
        if (scale.length < 3) {
            scale = new double[]{0.8, 0.8, 0.8};
        }
        double price = section.getDouble("price.amount", 0);
        String priceCurrency = section.getString("price.currency", "gold");

        String keyMaterial = section.getString("key.material", "TRIPWIRE_HOOK");
        String keyName = section.getString("key.name");
        List<String> keyLore = section.getStringList("key.lore");

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
        return new Crate(id, displayName, displayLore, engineType, vanillaItem, customDisplayItem, blockMaterial,
                scale, price, priceCurrency, keyMaterial, keyName, keyLore, idle, opening, rewards);
    }

    /** Sorteia uma recompensa por chance acumulada dentre TODAS as rewards da crate. */
    public Reward rollReward() {
        return rollFrom(rewards);
    }

    /** Sorteia uma recompensa por chance acumulada dentro de um subconjunto (ex: pool elegivel ja filtrado). */
    public static Reward rollFrom(List<Reward> pool) {
        if (pool.isEmpty()) {
            return null;
        }
        double total = 0;
        for (Reward reward : pool) {
            total += reward.getChance();
        }
        if (total <= 0) {
            return pool.get(0);
        }
        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cumulative = 0;
        for (Reward reward : pool) {
            cumulative += reward.getChance();
            if (roll <= cumulative) {
                return reward;
            }
        }
        return pool.get(pool.size() - 1);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getDisplayLore() { return displayLore; }
    public CrateEngineType getEngineType() { return engineType; }
    public String getVanillaItem() { return vanillaItem; }
    public ItemStack getCustomDisplayItem() { return customDisplayItem; }
    public String getBlockMaterial() { return blockMaterial; }
    public double[] getScale() { return scale; }
    public double getPrice() { return price; }
    public String getPriceCurrency() { return priceCurrency; }
    public String getKeyMaterial() { return keyMaterial; }
    public String getKeyName() { return keyName; }
    public List<String> getKeyLore() { return keyLore; }
    public IdleAnimation getIdleAnimation() { return idleAnimation; }
    public OpeningSequence getOpeningSequence() { return openingSequence; }
    public List<Reward> getRewards() { return rewards; }
    public List<Reward> getGuaranteedRewards() { return guaranteedRewards; }
}
