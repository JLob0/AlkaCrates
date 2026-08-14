package com.alkacode.crates.hook.item;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Hook do MMOItems via reflection. Padrao atual:
 * MMOItems.plugin().getAPI().getMMOItem(type, id).newBuilder(amount).build().
 */
public final class MMOItemsHook implements ItemHook {

    private static boolean failed = false;
    private static Method getMmoItemMethod;
    private static Object api;

    public MMOItemsHook() {
        try {
            Class<?> pluginClass = Class.forName("net.Indyuce.mmoitems.MMOItems");
            Method getPlugin = pluginClass.getMethod("plugin");
            Object plugin = getPlugin.invoke(null);
            Class<?> apiClass = Class.forName("net.Indyuce.mmoitems.api.MMOItemsAPI");
            Method getApi = apiClass.getMethod("getInstance");
            api = getApi.invoke(null);
            getMmoItemMethod = apiClass.getMethod("getMMOItem", String.class, String.class);
        } catch (Throwable t) {
            failed = true;
        }
    }

    public static boolean isAvailable() {
        return !failed && api != null && getMmoItemMethod != null;
    }

    @Override
    public String getPrefix() {
        return "mmoitems";
    }

    @Override
    public boolean matches(String id) {
        return id != null && id.startsWith("mmoitems:");
    }

    @Override
    public ItemStack resolve(String id) {
        if (!isAvailable() || !matches(id)) {
            return null;
        }
        try {
            // formato: mmoitems:<type>:<id>  ou  mmoitems:<id> (type default SWORD)
            String raw = id.substring("mmoitems:".length());
            String[] parts = raw.split(":", 2);
            String type = "SWORD";
            String itemId = parts[0];
            if (parts.length == 2) {
                type = parts[0];
                itemId = parts[1];
            }
            Object mmoItem = getMmoItemMethod.invoke(api, type, itemId);
            if (mmoItem == null) {
                return null;
            }
            Method newBuilder = mmoItem.getClass().getMethod("newBuilder", int.class);
            Object builder = newBuilder.invoke(mmoItem, 1);
            Method build = builder.getClass().getMethod("build");
            Object item = build.invoke(builder);
            return item instanceof ItemStack ? (ItemStack) item : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
