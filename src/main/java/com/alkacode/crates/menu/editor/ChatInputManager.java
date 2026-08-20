package com.alkacode.crates.menu.editor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Fila "proxima mensagem de chat vira input" - mesmo padrao ja usado em AlkaItems/AlkaAnvil,
 * copiado aqui (nao extraido pro AlkaCore por uma classe de ~20 linhas). */
public final class ChatInputManager {

    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public void await(UUID uuid, Consumer<String> callback) {
        pending.put(uuid, callback);
    }

    public boolean isAwaiting(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public void complete(UUID uuid, String input) {
        Consumer<String> callback = pending.remove(uuid);
        if (callback != null) {
            callback.accept(input);
        }
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }
}
