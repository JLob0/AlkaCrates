package com.alkacode.crates.animation;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Uma fase da sequencia de abertura. */
public final class Phase {

    private final String name;
    private final double duration;
    private final Transform transform;
    private final Easing easing;
    private final List<ParticleEffect> particles;
    private final Sound sound;
    private final PhaseTrigger trigger;

    public Phase(String name, double duration, Transform transform, Easing easing,
                 List<ParticleEffect> particles, Sound sound, PhaseTrigger trigger) {
        this.name = name;
        this.duration = duration;
        this.transform = transform;
        this.easing = easing == null ? Easing.LINEAR : easing;
        this.particles = particles;
        this.sound = sound;
        this.trigger = trigger;
    }

    public static Phase from(ConfigurationSection section) {
        String name = section.getString("name", "phase");
        double duration = section.getDouble("duration", 1);
        Transform transform = Transform.identity();
        ConfigurationSection t = section.getConfigurationSection("transform");
        if (t != null) {
            transform = new Transform(
                    t.getDoubleList("offset").stream().mapToDouble(Double::doubleValue).toArray(),
                    t.getDoubleList("rotation").stream().mapToDouble(Double::doubleValue).toArray(),
                    t.getDoubleList("scale").stream().mapToDouble(Double::doubleValue).toArray());
        }
        Easing easing;
        try {
            easing = Easing.valueOf(section.getString("easing", "LINEAR").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            easing = Easing.LINEAR;
        }
        List<ParticleEffect> particles = new ArrayList<>();
        for (String key : section.getConfigurationSection("particles") != null
                ? section.getConfigurationSection("particles").getKeys(false)
                : new ArrayList<String>()) {
            particles.add(ParticleEffect.from(section.getConfigurationSection("particles").getConfigurationSection(key)));
        }
        Sound sound = null;
        String soundName = section.getString("sound");
        if (soundName != null) {
            sound = Registry.SOUND_EVENT.get(NamespacedKey.minecraft(soundName.toLowerCase(Locale.ROOT)));
        }
        PhaseTrigger trigger = PhaseTrigger.NONE;
        String triggerName = section.getString("trigger");
        if (triggerName != null) {
            try {
                trigger = PhaseTrigger.valueOf(triggerName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                trigger = PhaseTrigger.NONE;
            }
        }
        return new Phase(name, duration, transform, easing, particles, sound, trigger);
    }

    public String getName() { return name; }
    public double getDuration() { return duration; }
    public Transform getTransform() { return transform; }
    public Easing getEasing() { return easing; }
    public List<ParticleEffect> getParticles() { return particles; }
    public Sound getSound() { return sound; }
    public PhaseTrigger getTrigger() { return trigger; }
}
