package com.alkacode.crates.hook.item;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Hook do MythicItems (itens do MythicMobs) via reflection.
 * Padrao: io.lumine.mythic.bukkit.MythicBukkit.inst().getItemManager().getItemStack(id).
 */
public final class MythicItemsHook implements ItemHook {

    private static boolean failed = false;
    private static Method getItemStackMethod;
    private static Object itemManager;

    public MythicItemsHook() {
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method inst = mythicBukkitClass.getMethod("inst");
            Object mythic = inst.invoke(null);
            if (mythic == null) {
                failed = true;
                return;
            }
            Method getItemManager = mythic.getClass().getMethod("getItemManager");
            itemManager = getItemManager.invoke(mythic);
            getItemStackMethod = itemManager.getClass().getMethod("getItemStack", String.class);
        } catch (Throwable t) {
            failed = true;
        }
    }

    public static boolean isAvailable() {
        return !failed && itemManager != null && getItemStackMethod != null;
    }

    @Override
    public String getPrefix() {
        return "mythicitems";
    }

    @Override
    public boolean matches(String id) {
        return id != null && id.startsWith("mythicitems:");
    }

    @Override
    public ItemStack resolve(String id) {
        if (!isAvailable() || !matches(id)) {
            return null;
        }
        try {
            String rawId = id.substring("mythicitems:".length());
            Object item = getItemStackMethod.invoke(itemManager, rawId);
            return item instanceof ItemStack ? (ItemStack) item : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
