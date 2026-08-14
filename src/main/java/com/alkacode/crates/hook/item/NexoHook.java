package com.alkacode.crates.hook.item;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/** Hook do Nexo (item engine) via reflection. */
public final class NexoHook implements ItemHook {

    private static boolean failed = false;
    private static Method getItemMethod;

    public NexoHook() {
        try {
            Class<?> nexoItemClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            getItemMethod = nexoItemClass.getMethod("itemFromId", String.class);
        } catch (Throwable t) {
            failed = true;
        }
    }

    public static boolean isAvailable() {
        return !failed && getItemMethod != null;
    }

    @Override
    public String getPrefix() {
        return "nexo";
    }

    @Override
    public boolean matches(String id) {
        return id != null && id.startsWith("nexo:");
    }

    @Override
    public ItemStack resolve(String id) {
        if (!isAvailable() || !matches(id)) {
            return null;
        }
        try {
            String rawId = id.substring("nexo:".length());
            Object item = getItemMethod.invoke(null, rawId);
            return item instanceof ItemStack ? (ItemStack) item : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
