package com.alkacode.crates.display;

import com.alkacode.crates.crate.model.Crate;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entidade visual da crate no mundo. Agrupa um ItemDisplay (a "fonte"), um
 * TextDisplay (nome/status) e uma Interaction (hitbox). Pode ter tambem uma
 * entidade modelo adicional (ModelEngine/BetterModel/CraftEngine) como extra.
 * O scheduler de animacao roda na main thread via Bukkit.
 */
public final class CrateDisplay {

    private final Crate crate;
    private final Location location;
    private final ItemDisplay itemDisplay;
    private final TextDisplay textDisplay;
    private final Interaction interaction;
    private final float[] baseScale;
    private Entity modelEntity;
    private final Map<UUID, BukkitRunnable> rewardTasks = new HashMap<>();

    public CrateDisplay(Crate crate, Location location, ItemStack item,
                        String hologram, float[] baseScale, float interactionWidth, float interactionHeight) {
        this.crate = crate;
        this.location = location.clone();
        this.baseScale = baseScale;
        this.itemDisplay = DisplayEntityFactory.createItemDisplay(location, item, baseScale);
        this.textDisplay = DisplayEntityFactory.createHologram(location.clone().add(0, 0.8, 0), hologram);
        this.interaction = DisplayEntityFactory.createInteraction(location, interactionWidth, interactionHeight);
    }

    public void applyTransform(double[] offset, double[] rotation, double[] scale) {
        if (itemDisplay == null || !itemDisplay.isValid()) {
            return;
        }
        Vector3f translation = new Vector3f(
                (float) offset[0], (float) offset[1], (float) offset[2]);
        AxisAngle4f left = new AxisAngle4f(
                (float) Math.toRadians(rotation[0]),
                (float) Math.toRadians(rotation[1]),
                (float) Math.toRadians(rotation[2]), 1);
        Vector3f scl = new Vector3f((float) scale[0], (float) scale[1], (float) scale[2]);
        itemDisplay.setTransformation(new Transformation(translation, left, scl, new AxisAngle4f(0, 0, 0, 1)));
    }

    /** Troca o item temporariamente (reward reveal), depois de `ticks` restaura o original. */
    public void showReward(ItemStack item, int ticks, ItemStack original) {
        if (itemDisplay == null || !itemDisplay.isValid()) {
            return;
        }
        itemDisplay.setItemStack(item);
        float[] bigScale = new float[]{baseScale[0] * 2, baseScale[1] * 2, baseScale[2] * 2};
        applyTransform(new double[]{0, 1.5, 0}, new double[]{0, 0, 0}, new double[]{bigScale[0], bigScale[1], bigScale[2]});

        UUID id = itemDisplay.getUniqueId();
        BukkitRunnable existing = rewardTasks.get(id);
        if (existing != null) {
            existing.cancel();
        }
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (itemDisplay.isValid()) {
                    itemDisplay.setItemStack(original);
                    applyTransform(new double[]{0, 0, 0}, new double[]{0, 0, 0},
                            new double[]{baseScale[0], baseScale[1], baseScale[2]});
                }
                rewardTasks.remove(id);
            }
        };
        task.runTaskLater(ownerPlugin(), ticks);
        rewardTasks.put(id, task);
    }

    private org.bukkit.plugin.Plugin ownerPlugin() {
        return org.bukkit.Bukkit.getPluginManager().getPlugin("AlkaCrates");
    }

    public void setModelEntity(Entity entity) {
        this.modelEntity = entity;
    }

    public Entity getModelEntity() {
        return modelEntity;
    }

    public void remove() {
        for (BukkitRunnable task : rewardTasks.values()) {
            task.cancel();
        }
        rewardTasks.clear();
        if (modelEntity != null) {
            modelEntity.remove();
            modelEntity = null;
        }
        if (itemDisplay != null) itemDisplay.remove();
        if (textDisplay != null) textDisplay.remove();
        if (interaction != null) interaction.remove();
    }

    public boolean isValid() {
        return itemDisplay != null && itemDisplay.isValid();
    }

    public Crate getCrate() { return crate; }
    public Location getLocation() { return location; }
    public ItemDisplay getItemDisplay() { return itemDisplay; }
    public TextDisplay getTextDisplay() { return textDisplay; }
    public Interaction getInteraction() { return interaction; }
}
