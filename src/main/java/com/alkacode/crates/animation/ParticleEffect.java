package com.alkacode.crates.animation;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/**
 * Config de uma emissao de particula. Por padrao (shape=NONE) e um burst simples
 * num offset fixo (comportamento original). Com "shape:" configurado, os pontos
 * sao calculados AO VIVO por formula geometrica a cada tick (ver
 * {@link ParticleShapes}) - portado do DadaCratesPro (2026-09-03), zero keyframe
 * manual necessario pra formas tipo anel/coracao/espiral/etc.
 */
public final class ParticleEffect {

    private final Particle particle;
    private final int count;
    private final double[] offset;
    private final double speed;
    private final int interval;
    private final ParticleShape shape;
    private final int shapePoints;
    private final double shapeRadius;
    private final float dustSize;

    public ParticleEffect(Particle particle, int count, double[] offset, double speed, int interval,
                           ParticleShape shape, int shapePoints, double shapeRadius, float dustSize) {
        this.particle = particle;
        this.count = count;
        this.offset = offset;
        this.speed = speed;
        this.interval = interval;
        this.shape = shape;
        this.shapePoints = shapePoints;
        this.shapeRadius = shapeRadius;
        this.dustSize = dustSize;
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
        ParticleShape shape = ParticleShape.parse(section.getString("shape"));
        int shapePoints = section.getInt("points", 20);
        double shapeRadius = section.getDouble("radius", 0.8);
        float dustSize = (float) section.getDouble("dust-size", 1.0);
        return new ParticleEffect(particle, count, offset, speed, interval, shape, shapePoints, shapeRadius, dustSize);
    }

    public Particle getParticle() { return particle; }
    public int getCount() { return count; }
    public double[] getOffset() { return offset; }
    public double getSpeed() { return speed; }
    public int getInterval() { return interval; }
    public ParticleShape getShape() { return shape; }
    public int getShapePoints() { return shapePoints; }
    public double getShapeRadius() { return shapeRadius; }
    public float getDustSize() { return dustSize; }
}
