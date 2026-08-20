package com.alkacode.crates.engine;

import com.alkacode.crates.crate.model.Crate;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;

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

    /** Linhas de dica de clique - cada engine sabe quais botoes fazem o que (ver CrateInteractionListener). */
    default List<String> interactionHints() {
        return List.of("<yellow>➜ <gray>Direito: <gold>Abrir caixa");
    }

    /**
     * Titulo + lore (display.lore) + dicas de clique, compostos num holograma multi-linha
     * (TextDisplay aceita \n). A fonte "pixelada" de servidores com resource pack proprio
     * nao da pra replicar aqui (isso e textura/fonte do cliente, fora do alcance do plugin) -
     * mas cor, negrito, espacamento e icones (♦/➜/✦, glifos que o MC vanilla renderiza sem
     * pack nenhum) sim.
     */
    default String hologramText(Crate crate) {
        StringBuilder sb = new StringBuilder("<bold>").append(crate.getDisplayName()).append("</bold>");
        for (String line : crate.getDisplayLore()) {
            sb.append('\n').append(line);
        }
        sb.append("\n<gray><italic>Clique para interagir.");
        sb.append("\n ");
        for (String hint : interactionHints()) {
            sb.append('\n').append(hint);
        }
        return sb.toString();
    }
}
