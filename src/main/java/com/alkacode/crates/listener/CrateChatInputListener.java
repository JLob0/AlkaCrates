package com.alkacode.crates.listener;

import com.alkacode.crates.menu.editor.ChatInputManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/** Roteia a proxima mensagem de chat de um admin "aguardando input" pro ChatInputManager do editor de crates. */
public final class CrateChatInputListener implements Listener {

    private final JavaPlugin plugin;
    private final ChatInputManager chatInputManager;

    public CrateChatInputListener(JavaPlugin plugin, ChatInputManager chatInputManager) {
        this.plugin = plugin;
        this.chatInputManager = chatInputManager;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        if (!chatInputManager.isAwaiting(uuid)) {
            return;
        }
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        Bukkit.getScheduler().runTask(plugin, () -> chatInputManager.complete(uuid, message));
    }
}
