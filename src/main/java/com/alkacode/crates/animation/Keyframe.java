package com.alkacode.crates.animation;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/** Um ponto da timeline: instante, transform e easing para chegar nele. */
public final class Keyframe {

    private final double time;
    private final Transform transform;
    private final Easing easing;

    public Keyframe(double time, Transform transform, Easing easing) {
        this.time = time;
        this.transform = transform;
        this.easing = easing == null ? Easing.LINEAR : easing;
    }

    public static Keyframe from(ConfigurationSection section) {
        double time = section.getDouble("time", 0);
        Transform transform = Transform.identity();
        ConfigurationSection t = section.getConfigurationSection("transform");
        if (t != null) {
            transform = new Transform(
                    t.getDoubleList("offset").stream().mapToDouble(Double::doubleValue).toArray(),
                    t.getDoubleList("rotation").stream().mapToDouble(Double::doubleValue).toArray(),
                    t.getDoubleList("scale").stream().mapToDouble(Double::doubleValue).toArray());
        }
        String easingName = section.getString("easing", "LINEAR").toUpperCase(Locale.ROOT);
        Easing easing;
        try {
            easing = Easing.valueOf(easingName);
        } catch (IllegalArgumentException e) {
            easing = Easing.LINEAR;
        }
        return new Keyframe(time, transform, easing);
    }

    public double getTime() { return time; }
    public Transform getTransform() { return transform; }
    public Easing getEasing() { return easing; }
}
