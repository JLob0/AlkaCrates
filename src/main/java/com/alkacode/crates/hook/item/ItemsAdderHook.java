package com.alkacode.crates.hook.item;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Hook do ItemsAdder via reflection - nao depende do jar em compile-time (soft
 * depend). Usa CustomStack.getInstance(id) e byItemStack. Se a API mudar, so este
 * arquivo precisa de ajuste.
 */
public final class ItemsAdderHook implements ItemHook {

    private static Class<?> customStackClass;
    private static Method getInstance;
    private static Method byItemStack;
    private static boolean failed = false;

    public ItemsAdderHook() {
        try {
            customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            getInstance = customStackClass.getMethod("getInstance", String.class);
            byItemStack = customStackClass.getMethod("byItemStack", ItemStack.class);
        } catch (Throwable t) {
            failed = true;
        }
    }

    public static boolean isAvailable() {
        return !failed && customStackClass != null;
    }

    @Override
    public String getPrefix() {
        return "itemsadder";
    }

    @Override
    public boolean matches(String id) {
        return id != null && id.startsWith("itemsadder:");
    }

    @Override
    public ItemStack resolve(String id) {
        if (!isAvailable() || !matches(id)) {
            return null;
        }
        try {
            String rawId = id.substring("itemsadder:".length());
            Object stack = getInstance.invoke(null, rawId);
            if (stack == null) {
                return null;
            }
            Method getItemStack = stack.getClass().getMethod("getItemStack");
            Object item = getItemStack.invoke(stack);
            return item instanceof ItemStack ? (ItemStack) item : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
