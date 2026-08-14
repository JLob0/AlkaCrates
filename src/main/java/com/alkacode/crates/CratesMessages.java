package com.alkacode.crates;

import com.alkacode.core.api.MessageProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

/**
 * Provider de mensagens proprio do AlkaCrates. O MessageProvider do AlkaCore nao
 * expoe getMessage(chave) - ele so parseia strings MiniMessage com prefixo do
 * config do Core. Aqui carregamos o messages.yml do proprio AlkaCrates e fazemos
 * substituicao de placeholders antes de renderizar via MessageProvider#parse.
 */
public final class CratesMessages {

    private final AlkaCrates plugin;
    private final MessageProvider messageProvider;
    private YamlConfiguration config;

    public CratesMessages(AlkaCrates plugin) {
        this.plugin = plugin;
        this.messageProvider = plugin.getAlkaAPI().getMessages();
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    private String prefix() {
        return config.getString("prefix", "<gradient:#FFD700:#FFA500>[AlkaCrates]</gradient> ");
    }

    /** Pega a mensagem crua, aplica {prefix} e os placeholders dados, e renderiza em Component. */
    public Component parse(String key, Map<String, String> placeholders) {
        String raw = config.getString("messages." + key, key);
        if (raw == null) {
            return messageProvider.parse(key);
        }
        raw = raw.replace("{prefix}", prefix());
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return messageProvider.parse(raw);
    }

    public Component parse(String key) {
        return parse(key, null);
    }

    public void send(CommandSender target, String key) {
        target.sendMessage(parse(key));
    }

    public void send(CommandSender target, String key, Map<String, String> placeholders) {
        target.sendMessage(parse(key, placeholders));
    }
}
