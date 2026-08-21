package com.alkacode.crates.menu;

import com.alkacode.core.gui.BaseGui;
import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.KeyType;
import com.alkacode.crates.gui.layout.GuiLayoutLoader;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
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
        GuiLayoutLoader.GuiLayout layout = plugin.getGuiLayoutLoader().getLayout("alkacrates-backpack");
        fillBorder(plugin.getMenuConfig().item("common.border", null));

        List<Integer> slots = layout.findSlots('0');
        List<ItemStack> icons = new ArrayList<>();

        for (Crate crate : plugin.getCratesConfig().getCrates()) {
            int physical = plugin.getKeyService().getKeyCount(player, crate.getId(), KeyType.PHYSICAL);
            int virtual = plugin.getKeyService().getKeyCount(player, crate.getId(), KeyType.VIRTUAL);
            if (physical <= 0 && virtual <= 0) {
                continue;
            }
            if (icons.size() >= slots.size()) {
                break;
            }
            Material icon = Material.matchMaterial(crate.getVanillaItem());
            List<String> lore = plugin.getMenuConfig().rawLore("alkacrates-backpack.key-item", Map.of(
                    "fisicas", String.valueOf(physical),
                    "virtuais", String.valueOf(virtual),
                    "total", String.valueOf(physical + virtual)));
            ItemStack item = createItem(icon != null ? icon : Material.TRIPWIRE_HOOK,
                    "<gold><bold>" + crate.getDisplayName(), lore.toArray(new String[0]));
            int index = icons.size();
            setItem(slots.get(index), item, event -> {
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
            icons.add(item);
        }

        if (icons.isEmpty()) {
            // Mesma posicao visual do placeholder antigo (slot 13, centro da 1a linha de conteudo).
            int emptySlot = slots.get(3);
            setItem(emptySlot, plugin.getMenuConfig().item("alkacrates-backpack.vazio", null));
        }
    }
}
