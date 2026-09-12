package com.alkacode.crates.hook;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ponte com o AdvancedEnchantments via reflection - plugin pago sem artefato Maven
 * publico. Assinaturas confirmadas via javap no jar real, ver
 * [[reference-advancedenchantments-api]] - a classe {@code AEAPI} e 100% estatica,
 * {@code applyEnchant} retorna um ItemStack NOVO (nao muta o parametro in-place).
 * Mesmo padrao ja usado em AlkaVips/AlkaMines/AlkaAnvil - NUNCA reimplementar
 * limite/conflito de nivel aqui, o AE ja valida isso sozinho dentro de applyEnchant.
 */
public final class AdvancedEnchantmentsHook {

    private static final String API_CLASS = "net.advancedplugins.ae.api.AEAPI";

    private static Method applyEnchantMethod;
    private static Method isAnEnchantmentMethod;
    private static Method createEnchantmentBookMethod;
    private static boolean loaded;

    private AdvancedEnchantmentsHook() {
    }

    /** Chama 1x no onEnable. */
    public static synchronized boolean init(Logger logger) {
        if (loaded) {
            return true;
        }
        if (Bukkit.getPluginManager().getPlugin("AdvancedEnchantments") == null) {
            return false;
        }
        try {
            Class<?> api = Class.forName(API_CLASS);
            applyEnchantMethod = api.getMethod("applyEnchant", String.class, int.class, ItemStack.class);
            isAnEnchantmentMethod = api.getMethod("isAnEnchantment", String.class);
            // Assinatura confirmada em https://ae.advancedplugins.net/for-developers/plugin-api
            // (createEnchantmentBook(String enchant, int level, int success, int failure) - SEM
            // Player, diferente do que uma leitura anterior via javap tinha sugerido).
            createEnchantmentBookMethod = api.getMethod("createEnchantmentBook",
                    String.class, int.class, int.class, int.class);
            loaded = true;
            logger.info("AdvancedEnchantments detectado - ae-book habilitado nas rewards de crate.");
            return true;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "AdvancedEnchantments encontrado mas a API nao carregou via reflexao.", t);
            return false;
        }
    }

    public static boolean isAvailable() {
        return loaded;
    }

    /** Retorna false pra nomes desconhecidos - use pra validar ae-enchants no load da crate
     * e avisar no console ANTES de um jogador abrir a crate, em vez de falhar silenciosamente
     * so na hora de dar o premio. */
    public static boolean isAnEnchantment(String name) {
        if (!loaded || name == null || name.isBlank()) {
            return false;
        }
        try {
            Object result = isAnEnchantmentMethod.invoke(null, name);
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Aplica o encantamento AE no item, retornando o ItemStack resultante (a API do AE
     * devolve um item NOVO em vez de mutar in-place). Nunca lanca - se falhar, devolve o
     * item original sem o encantamento (log em WARNING pra ficar visivel). */
    public static ItemStack applyEnchant(Logger logger, ItemStack item, String aeName, int level) {
        if (!loaded) {
            return item;
        }
        try {
            Object result = applyEnchantMethod.invoke(null, aeName, level, item);
            return result instanceof ItemStack stack ? stack : item;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Falha ao aplicar encantamento AE '" + aeName + "' numa reward de crate.", t);
            return item;
        }
    }

    /**
     * Cria um livro de encantamento de VERDADE do AE (formato proprio do AE - reconhecido
     * pelo drag-n-drop nativo dele, diferente de um ENCHANTED_BOOK vanilla comum). Retorna
     * null se o AE nao estiver disponivel, o nome do encantamento nao existir, ou a
     * chamada falhar por qualquer motivo (log em WARNING) - o chamador deve cair pro
     * ENCHANTED_BOOK generico nesse caso, nunca travar a entrega da reward.
     */
    public static ItemStack createEnchantmentBook(Logger logger, String enchant, int level, int success, int failure) {
        if (!loaded) {
            return null;
        }
        if (!isAnEnchantment(enchant)) {
            logger.warning("Reward de crate referencia encantamento AE desconhecido: '" + enchant
                    + "' - confirme o nome exato via AEAPI#getAllEnchantments().");
            return null;
        }
        try {
            Object result = createEnchantmentBookMethod.invoke(null, enchant, level, success, failure);
            return result instanceof ItemStack stack ? stack : null;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Falha ao criar livro AE '" + enchant + "' numa reward de crate.", t);
            return null;
        }
    }
}
