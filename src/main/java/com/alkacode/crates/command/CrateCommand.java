package com.alkacode.crates.command;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.menu.KeyBackpackMenu;
import com.alkacode.crates.menu.PreviewMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** /crate (alias /crates) - comandos do jogador: preview, mochila. Keys so via evento/admin. */
public final class CrateCommand implements CommandExecutor, TabCompleter {

    private final AlkaCrates plugin;

    public CrateCommand(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponivel apenas in-game.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("/crate preview <crate> | /crate mochila");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "preview" -> {
                if (args.length < 2) {
                    player.sendMessage("Uso: /crate preview <crate>");
                    return true;
                }
                Crate crate = plugin.getCratesConfig().getCrate(args[1]);
                if (crate == null) {
                    plugin.getCratesMessages().send(player, "crate-not-found", Map.of("crate", args[1]));
                    return true;
                }
                new PreviewMenu(plugin, player, crate).open();
                return true;
            }
            case "mochila" -> {
                new KeyBackpackMenu(plugin, player).open();
                return true;
            }
            default -> player.sendMessage("Subcomando invalido.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("preview");
            completions.add("mochila");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            plugin.getCratesConfig().getCrates().forEach(c -> completions.add(c.getId()));
        }
        return completions;
    }
}
