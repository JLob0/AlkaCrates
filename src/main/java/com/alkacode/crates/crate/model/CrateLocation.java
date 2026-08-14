package com.alkacode.crates.crate.model;

import org.bukkit.Location;
import org.bukkit.World;

/** Representa onde uma crate fisica esta no mundo (persistido no DB). */
public final class CrateLocation {

    private final String crateId;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public CrateLocation(String crateId, String world, double x, double y, double z, float yaw, float pitch) {
        this.crateId = crateId;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static CrateLocation fromLocation(String crateId, Location location) {
        return new CrateLocation(crateId, location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public Location toLocation() {
        World w = org.bukkit.Bukkit.getWorld(world);
        if (w == null) {
            return null;
        }
        return new Location(w, x, y, z, yaw, pitch);
    }

    public String getCrateId() { return crateId; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
}
