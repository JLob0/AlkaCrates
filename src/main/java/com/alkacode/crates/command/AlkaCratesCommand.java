package com.alkacode.crates.command;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.KeyType;
import com.alkacode.crates.crate.placement.PlacedCrate;
import com.alkacode.crates.menu.AdminMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
            sender.sendMessage("/alkacrates place <crate> | remove [crate:tag] | list | reload | "
                    + "givekey <player> <crate> <amount> [--virtual]");
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
                String tag = plugin.getPlacementService().placeAt(crate, placementLocation(target));
                if (tag != null) {
                    plugin.getCratesMessages().send(sender, "crate-placed",
                            Map.of("crate", crate.getDisplayName(), "tag", tag));
                } else {
                    plugin.getCratesMessages().send(sender, "crate-invalid-location");
                }
                return true;
            }
            case "remove" -> {
                // /alkacrates remove <crate>:<tag> - remove remotamente, sem precisar estar perto.
                if (args.length >= 2 && args[1].contains(":")) {
                    String[] parts = args[1].split(":", 2);
                    boolean removedByTag = plugin.getPlacementService().removeByTag(parts[0], parts[1]);
                    if (removedByTag) {
                        plugin.getCratesMessages().send(sender, "crate-removed", Map.of("crate", parts[0] + ":" + parts[1]));
                    } else {
                        plugin.getCratesMessages().send(sender, "crate-tag-not-found", Map.of("tag", args[1]));
                    }
                    return true;
                }
                // sem argumento: fluxo antigo, mirando o bloco.
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Uso: /alkacrates remove <crate>:<tag> (ou mire no bloco, in-game)");
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
            case "list" -> {
                List<PlacedCrate> placedCrates = plugin.getPlacedCrateManager().getAll();
                if (placedCrates.isEmpty()) {
                    plugin.getCratesMessages().send(sender, "crate-list-empty");
                    return true;
                }
                plugin.getCratesMessages().send(sender, "crate-list-header", Map.of("amount", String.valueOf(placedCrates.size())));
                for (PlacedCrate placed : placedCrates) {
                    Location loc = placed.getLocation();
                    plugin.getCratesMessages().send(sender, "crate-list-entry", Map.of(
                            "tag", placed.getTag() != null ? placed.getTag() : "?",
                            "crate", placed.getCrate().getId(),
                            "world", loc.getWorld() != null ? loc.getWorld().getName() : "?",
                            "x", String.valueOf(loc.getBlockX()),
                            "y", String.valueOf(loc.getBlockY()),
                            "z", String.valueOf(loc.getBlockZ())));
                }
                return true;
            }
            case "reload" -> {
                plugin.reloadEverything();
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

    /**
     * Localizacao onde a crate fica: bloco ACIMA do bloco mirado, centralizado.
     * Assim a crate assenta sobre o topo do bloco (ex.: grama) em vez de entrar nele.
     */
    private Location placementLocation(Block target) {
        return target.getRelative(org.bukkit.block.BlockFace.UP)
                .getLocation().add(0.5, 0, 0.5);
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
            completions.add("list");
            completions.add("reload");
            completions.add("givekey");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("givekey") || args[0].equalsIgnoreCase("place")) {
                if (args[0].equalsIgnoreCase("givekey")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        completions.add(p.getName());
                    }
                } else {
                    plugin.getCratesConfig().getCrates().forEach(c -> completions.add(c.getId()));
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
                // sugere "crate:AC-###" pra cada crate ja colocada
                plugin.getPlacedCrateManager().getAll().forEach(placed ->
                        completions.add(placed.getCrate().getId() + ":" + placed.getTag()));
            }
        } else if (args.length == 3) {
            plugin.getCratesConfig().getCrates().forEach(c -> completions.add(c.getId()));
        } else if (args.length == 5) {
            completions.add("--virtual");
        }
        return completions;
    }
}
