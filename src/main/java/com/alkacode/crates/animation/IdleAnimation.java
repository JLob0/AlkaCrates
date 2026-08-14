package com.alkacode.crates.animation;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/** Animacao de idle (loop) configurada para a crate. */
public final class IdleAnimation {

    private final boolean loop;
    private final List<Keyframe> keyframes;
    private final List<ParticleEffect> particles;
    private final double totalDuration;

    public IdleAnimation(boolean loop, List<Keyframe> keyframes, List<ParticleEffect> particles) {
        this.loop = loop;
        this.keyframes = keyframes;
        this.particles = particles;
        double last = 0;
        for (Keyframe kf : keyframes) {
            if (kf.getTime() > last) {
                last = kf.getTime();
            }
        }
        this.totalDuration = last;
    }

    public static IdleAnimation from(ConfigurationSection section) {
        boolean loop = section.getBoolean("loop", true);
        List<Keyframe> keyframes = new ArrayList<>();
        ConfigurationSection kfSection = section.getConfigurationSection("keyframes");
        if (kfSection != null) {
            for (String key : kfSection.getKeys(false)) {
                keyframes.add(Keyframe.from(kfSection.getConfigurationSection(key)));
            }
        }
        List<ParticleEffect> particles = new ArrayList<>();
        ConfigurationSection pSection = section.getConfigurationSection("particles");
        if (pSection != null) {
            for (String key : pSection.getKeys(false)) {
                particles.add(ParticleEffect.from(pSection.getConfigurationSection(key)));
            }
        }
        return new IdleAnimation(loop, keyframes, particles);
    }

    public boolean isLoop() { return loop; }
    public List<Keyframe> getKeyframes() { return keyframes; }
    public List<ParticleEffect> getParticles() { return particles; }
    public double getTotalDuration() { return totalDuration; }
}
