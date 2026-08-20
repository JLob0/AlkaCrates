package com.alkacode.crates.display;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/** Fabrica de display entities vanilla (ItemDisplay, TextDisplay, Interaction). */
public final class DisplayEntityFactory {

    private DisplayEntityFactory() {
    }

    /** Cria um ItemDisplay com escala definida. */
    public static ItemDisplay createItemDisplay(Location location, ItemStack item, float[] scale) {
        ItemDisplay display = (ItemDisplay) location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
        display.setItemStack(item);
        display.setDisplayWidth(1.0f);
        display.setDisplayHeight(1.0f);
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale[0], scale[1], scale[2]),
                new AxisAngle4f(0, 0, 0, 1)));
        display.setInterpolationDuration(1);
        display.setBillboard(ItemDisplay.Billboard.CENTER);
        return display;
    }

    /** Cria um TextDisplay holograma flutuante. */
    public static TextDisplay createHologram(Location location, String text) {
        return createHologram(location, text, 0.6f);
    }

    /** Cria um TextDisplay holograma flutuante com escala definida (menor = texto menor). */
    public static TextDisplay createHologram(Location location, String text, float scale) {
        TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
        display.text(MiniMessage.miniMessage().deserialize(text));
        display.setBillboard(TextDisplay.Billboard.CENTER);
        display.setSeeThrough(false);
        display.setShadowed(false);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(0));
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 0, 1)));
        return display;
    }

    /** Cria uma Interaction entity para hitbox de clique direito. */
    public static Interaction createInteraction(Location location, float width, float height) {
        Interaction interaction = (Interaction) location.getWorld().spawnEntity(location, EntityType.INTERACTION);
        interaction.setInteractionWidth(width);
        interaction.setInteractionHeight(height);
        // false = a entidade nunca entra no fluxo de combate (sem EntityDamageByEntityEvent,
        // sem "you ready your fists", sem qualquer efeito de knockback vanilla). So precisamos
        // de PlayerInteractEntityEvent (clique direito), que dispara independente disso.
        interaction.setResponsive(false);
        return interaction;
    }
}
