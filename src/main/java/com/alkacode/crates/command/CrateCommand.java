package com.alkacode.crates.command;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.KeyType;
import com.alkacode.crates.menu.PreviewMenu;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** /crate - comandos para jogadores. */
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
            player.sendMessage("/crate preview <crate> | /crate givekey <player> <crate> <amount> [--virtual]");
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
            case "givekey" -> {
                if (!player.hasPermission("alkacrates.admin")) {
                    plugin.getCratesMessages().send(player, "crate-no-permission");
                    return true;
                }
                return handleGiveKey(sender, args);
            }
            default -> player.sendMessage("Subcomando invalido.");
        }
        return true;
    }

    private boolean handleGiveKey(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("Uso: /crate givekey <player> <crate> <amount> [--virtual]");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getCratesMessages().send(sender, "crate-not-found", Map.of("crate", args[1]));
            return true;
        }
        Crate crate = plugin.getCratesConfig().getCrate(args[2]);
        if (crate == null) {
            plugin.getCratesMessages().send(sender, "crate-not-found", Map.of("crate", args[2]));
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Quantidade invalida.");
            return true;
        }
        KeyType type = args.length >= 5 && args[4].equalsIgnoreCase("--virtual")
                ? KeyType.VIRTUAL : KeyType.PHYSICAL;
        plugin.getKeyService().giveKey(target, crate.getId(), amount, type);
        plugin.getCratesMessages().send(sender, "crate-givekey-admin", Map.of(
                "player", target.getName(),
                "crate", crate.getDisplayName(),
                "amount", String.valueOf(amount)));
        if (sender != target) {
            plugin.getCratesMessages().send(target, type == KeyType.VIRTUAL ? "key-virtual-received" : "crate-givekey",
                    Map.of("crate", crate.getDisplayName(), "amount", String.valueOf(amount)));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("preview");
            completions.add("givekey");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("givekey")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            } else {
                plugin.getCratesConfig().getCrates().forEach(c -> completions.add(c.getId()));
            }
        } else if (args.length == 3) {
            plugin.getCratesConfig().getCrates().forEach(c -> completions.add(c.getId()));
        } else if (args.length == 5) {
            completions.add("--virtual");
        }
        return completions;
    }
}
