package com.alkacode.crates.animation;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/** Config de uma emissao de particula. */
public final class ParticleEffect {

    private final Particle particle;
    private final int count;
    private final double[] offset;
    private final double speed;
    private final int interval;

    public ParticleEffect(Particle particle, int count, double[] offset, double speed, int interval) {
        this.particle = particle;
        this.count = count;
        this.offset = offset;
        this.speed = speed;
        this.interval = interval;
    }

    public static ParticleEffect from(ConfigurationSection section) {
        Particle particle;
        try {
            particle = Particle.valueOf(section.getString("type", "FLAME").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            particle = Particle.FLAME;
        }
        int count = section.getInt("count", 1);
        double[] offset = section.getDoubleList("offset").stream().mapToDouble(Double::doubleValue).toArray();
        if (offset.length < 3) {
            offset = new double[]{0, 0, 0};
        }
        double speed = section.getDouble("speed", 0);
        int interval = section.getInt("interval", 1);
        return new ParticleEffect(particle, count, offset, speed, interval);
    }

    public Particle getParticle() { return particle; }
    public int getCount() { return count; }
    public double[] getOffset() { return offset; }
    public double getSpeed() { return speed; }
    public int getInterval() { return interval; }
}
