package com.alkacode.crates.hook.item;

import org.bukkit.inventory.ItemStack;

/** Abstrai resolucao de itens custom de qualquer plugin de item (ItemsAdder, Nexo, MMOItems...). */
public interface ItemHook {

    /** Prefixo no config, ex: "itemsadder". */
    String getPrefix();

    /** True se o id cru comeca com "<prefixo>:". */
    boolean matches(String id);

    /** Resolve o ItemStack a partir do id cru completo (ex: "itemsadder:espada"). Null se falhar. */
    ItemStack resolve(String id);
}
