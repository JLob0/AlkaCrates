package com.alkacode.crates.menu;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.KeyType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Banco de keys do jogador: mostra quantas fisicas/virtuais ele tem de cada crate e
 * deixa converter entre os dois lados (deposita = fisica vira virtual, saca = virtual
 * vira item fisico de novo). So lista crates com pelo menos 1 key de algum tipo -
 * e uma "carteira", nao um catalogo de todas as crates existentes.
 */
public final class KeyBackpackMenu extends BaseGui {

    private final AlkaCrates plugin;

    public KeyBackpackMenu(AlkaCrates plugin, Player player) {
        super(plugin, player, "<gradient:#FFD700:#FFA500>Mochila de Keys</gradient>", 4, "alkacrates-backpack");
        this.plugin = plugin;
    }

    @Override
    public void render() {
        fillBorder(createItem(Material.BLACK_STAINED_GLASS_PANE, " "));

        int slot = 10;
        for (Crate crate : plugin.getCratesConfig().getCrates()) {
            int physical = plugin.getKeyService().getKeyCount(player, crate.getId(), KeyType.PHYSICAL);
            int virtual = plugin.getKeyService().getKeyCount(player, crate.getId(), KeyType.VIRTUAL);
            if (physical <= 0 && virtual <= 0) {
                continue;
            }
            if (slot >= 26) {
                break;
            }
            Material icon = Material.matchMaterial(crate.getVanillaItem());
            setItem(slot, createItem(icon != null ? icon : Material.TRIPWIRE_HOOK,
                    "<gold><bold>" + crate.getDisplayName(),
                    "<gray>Fisicas: <white>" + physical,
                    "<gray>Virtuais: <white>" + virtual,
                    "<gray>Total: <yellow>" + (physical + virtual),
                    "",
                    "<green>Esquerdo: <white>deposita 1  <green>Shift+Esquerdo: <white>deposita tudo",
                    "<aqua>Direito: <white>saca 1  <aqua>Shift+Direito: <white>saca tudo"), event -> {
                String crateId = crate.getId();
                if (event.isLeftClick()) {
                    int amount = event.isShiftClick() ? physical : 1;
                    int deposited = plugin.getKeyService().depositPhysical(player, crateId, amount);
                    if (deposited <= 0) {
                        player.sendMessage(plugin.getCratesMessages().parse("backpack-nothing-to-deposit"));
                    } else {
                        player.sendMessage(plugin.getCratesMessages().parse("backpack-deposit-success",
                                Map.of("amount", String.valueOf(deposited), "crate", crate.getDisplayName())));
                    }
                } else if (event.isRightClick()) {
                    int amount = event.isShiftClick() ? virtual : 1;
                    int withdrawn = plugin.getKeyService().withdrawVirtual(player, crateId, amount);
                    if (withdrawn <= 0) {
                        player.sendMessage(plugin.getCratesMessages().parse("backpack-nothing-to-withdraw"));
                    } else {
                        player.sendMessage(plugin.getCratesMessages().parse("backpack-withdraw-success",
                                Map.of("amount", String.valueOf(withdrawn), "crate", crate.getDisplayName())));
                    }
                } else {
                    return;
                }
                refresh();
            });
            slot++;
        }

        if (slot == 10) {
            setItem(13, createItem(Material.BARRIER, "<red>Voce nao tem nenhuma key",
                    "<gray>Compre com <white>/crate buy <crate>",
                    "<gray>ou receba de um evento/admin."));
        }
    }
}
