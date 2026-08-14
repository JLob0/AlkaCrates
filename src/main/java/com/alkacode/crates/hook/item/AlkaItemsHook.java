package com.alkacode.crates.hook.item;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Hook do AlkaItems (itens customizados da propria networking) via reflection.
 * Usa AlkaItemsPlugin#getAPI() -> AlkaItemsAPI#getTemplate(id) e, para materializar
 * o ItemStack, AlkaItemsServices#itemService.build(template, amount). Regra da rede:
 * integracao sempre via reflection, nunca import compilado.
 */
public final class AlkaItemsHook implements ItemHook {

    private final org.bukkit.plugin.Plugin plugin;
    private Method getApiMethod;
    private Method getTemplateMethod;
    private Object api;

    public AlkaItemsHook(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
        try {
            getApiMethod = plugin.getClass().getMethod("getAPI");
            Object apiObj = getApiMethod.invoke(plugin);
            if (apiObj != null) {
                this.api = apiObj;
                this.getTemplateMethod = apiObj.getClass().getMethod("getTemplate", String.class);
            }
        } catch (Throwable t) {
            this.api = null;
        }
    }

    @Override
    public String getPrefix() {
        return "alkaitems";
    }

    @Override
    public boolean matches(String id) {
        return id != null && id.startsWith("alkaitems:");
    }

    @Override
    public ItemStack resolve(String id) {
        if (api == null || !matches(id)) {
            return null;
        }
        try {
            String rawId = id.substring("alkaitems:".length());
            Object template = getTemplateMethod.invoke(api, rawId);
            if (template == null) {
                return null;
            }
            // itemService.build(template, amount) - resolve via services do plugin
            Object services = findField(plugin, "services");
            if (services == null) {
                return null;
            }
            Object itemService = findField(services, "itemService");
            if (itemService == null) {
                return null;
            }
            Method build = itemService.getClass().getMethod("build", template.getClass(), int.class);
            Object item = build.invoke(itemService, template, 1);
            return item instanceof ItemStack ? (ItemStack) item : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private Object findField(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (Throwable t) {
            return null;
        }
    }
}
