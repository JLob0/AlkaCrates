package com.alkacode.crates.hook.item;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/** Hook do HeadDatabase via reflection. Resolve uma cabeca por id (base64 value). */
public final class HeadDatabaseHook implements ItemHook {

    private static boolean failed = false;
    private static Method getHeadMethod;

    public HeadDatabaseHook() {
        try {
            Class<?> apiClass = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
            getHeadMethod = apiClass.getMethod("getItemHead", String.class);
        } catch (Throwable t) {
            failed = true;
        }
    }

    public static boolean isAvailable() {
        return !failed && getHeadMethod != null;
    }

    @Override
    public String getPrefix() {
        return "headdb";
    }

    @Override
    public boolean matches(String id) {
        return id != null && id.startsWith("headdb:");
    }

    @Override
    public ItemStack resolve(String id) {
        if (!isAvailable() || !matches(id)) {
            return null;
        }
        try {
            String rawId = id.substring("headdb:".length());
            Object api = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI").getConstructor().newInstance();
            Object item = getHeadMethod.invoke(api, rawId);
            return item instanceof ItemStack ? (ItemStack) item : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
