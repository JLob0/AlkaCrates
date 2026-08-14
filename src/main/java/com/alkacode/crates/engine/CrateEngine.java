package com.alkacode.crates.engine;

import com.alkacode.crates.crate.model.Crate;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * Abstracao de engine visual. Cada engine cria a "fonte" visual da crate no mundo
 * e resolve o item de display. A VanillaEngine usa display entities; as demais
 * (ModelEngine/BetterModel/CraftEngine) delegam ao hook proprio.
 */
public interface CrateEngine {

    /** Cria o display visual da crate na localizacao e retorna o CrateDisplay. */
    com.alkacode.crates.display.CrateDisplay createDisplay(Crate crate, Location location);

    /** Resolve o ItemStack de exibicao (usado por menus/preview e fallback). */
    ItemStack resolveDisplayItem(Crate crate);

    CrateEngineType getType();
}
