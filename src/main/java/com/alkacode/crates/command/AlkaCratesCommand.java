package com.alkacode.crates.command;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.KeyType;
import com.alkacode.crates.menu.AdminMenu;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** /alkacrates - comandos administrativos. */
public final class AlkaCratesCommand implements CommandExecutor, TabCompleter {

    private final AlkaCrates plugin;

    public AlkaCratesCommand(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("alkacrates.admin")) {
            plugin.getCratesMessages().send(sender, "crate-no-permission");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/alkacrates place <crate> | remove | reload | givekey <player> <crate> <amount> [--virtual]");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "menu" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Disponivel apenas in-game.");
                    return true;
                }
                new AdminMenu(plugin, player).open();
                return true;
            }
            case "place" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Disponivel apenas in-game.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("Uso: /alkacrates place <crate>");
                    return true;
                }
                Crate crate = plugin.getCratesConfig().getCrate(args[1]);
                if (crate == null) {
                    plugin.getCratesMessages().send(sender, "crate-not-found", Map.of("crate", args[1]));
                    return true;
                }
                Block target = player.getTargetBlockExact(5);
                if (target == null) {
                    plugin.getCratesMessages().send(sender, "crate-invalid-location");
                    return true;
                }
                boolean ok = plugin.getPlacementService().placeAt(crate, target.getLocation().add(0.5, 0, 0.5), true);
                if (ok) {
                    plugin.getCratesMessages().send(sender, "crate-placed", Map.of("crate", crate.getDisplayName()));
                } else {
                    plugin.getCratesMessages().send(sender, "crate-invalid-location");
                }
                return true;
            }
            case "remove" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Disponivel apenas in-game.");
                    return true;
                }
                Block target = player.getTargetBlockExact(5);
                if (target == null) {
                    plugin.getCratesMessages().send(sender, "crate-invalid-location");
                    return true;
                }
                boolean removed = plugin.getPlacementService().removeAt(target.getLocation().add(0.5, 0, 0.5));
                if (removed) {
                    plugin.getCratesMessages().send(sender, "crate-removed", Map.of("crate", "crate"));
                } else {
                    plugin.getCratesMessages().send(sender, "crate-invalid-location");
                }
                return true;
            }
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getCratesConfig().load();
                plugin.getCratesMessages().reload();
                plugin.getPlacementService().loadAll();
                plugin.getCratesMessages().send(sender, "crate-reload");
                return true;
            }
            case "givekey" -> {
                return handleGiveKey(sender, args);
            }
            default -> sender.sendMessage("Subcomando invalido.");
        }
        return true;
    }

    private boolean handleGiveKey(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("Uso: /alkacrates givekey <player> <crate> <amount> [--virtual]");
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
            completions.add("menu");
            completions.add("place");
            completions.add("remove");
            completions.add("reload");
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
