package com.alkacode.crates.config;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.animation.IdleAnimation;
import com.alkacode.crates.animation.OpeningSequence;
import com.alkacode.crates.crate.model.Crate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/** Carrega crates de crates/*.yml e a animacao default do config.yml. */
public final class CratesConfig {

    private final AlkaCrates plugin;
    private final Map<String, Crate> crates = new HashMap<>();
    private IdleAnimation defaultIdle;
    private OpeningSequence defaultOpening;
    private boolean warnedMissingOpening;

    public CratesConfig(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    public void load() {
        crates.clear();
        ConfigurationSection defaultAnim = plugin.getConfig().getConfigurationSection("default-animation");
        if (defaultAnim != null) {
            if (defaultAnim.getConfigurationSection("idle") != null) {
                defaultIdle = IdleAnimation.from(defaultAnim.getConfigurationSection("idle"));
            }
            if (defaultAnim.getConfigurationSection("opening") != null) {
                defaultOpening = OpeningSequence.from(defaultAnim.getConfigurationSection("opening"));
            }
        }
        // A sequencia de abertura (Phase/Transform/AnimationEngine#playOpening) ficou
        // dormente - a abertura por key agora e instantanea, sem animacao (ver
        // CrateService). Mantido so pra nao perder o trabalho caso um "modo com
        // animacao" volte a ser pedido depois; por isso o fallback ainda existe, mas so
        // avisa 1x por sessao do plugin (senao cada edicao no GUI, que recarrega tudo,
        // spammava esse aviso a cada clique).
        if (defaultOpening == null || defaultOpening.getPhases().isEmpty()) {
            if (!warnedMissingOpening) {
                plugin.getLogger().warning("config.yml sem 'default-animation.opening' valido - isso nao afeta "
                        + "a abertura de crates (que e instantanea agora), so avisando 1x por sessao.");
                warnedMissingOpening = true;
            }
            defaultOpening = OpeningSequence.fallback();
        }

        File folder = new File(plugin.getDataFolder(), plugin.getConfig().getString("crates-folder", "crates"));
        if (!folder.exists()) {
            folder.mkdirs();
            createExample(folder);
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                Crate crate = Crate.from(config, defaultIdle, defaultOpening);
                crates.put(crate.getId(), crate);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Falha ao carregar crate " + file.getName(), e);
            }
        }
    }

    public Crate getCrate(String id) {
        return crates.get(id);
    }

    public List<Crate> getCrates() {
        return new ArrayList<>(crates.values());
    }

    private void createExample(File folder) {
        File file = new File(folder, "basica.yml");
        if (file.exists()) {
            return;
        }
        try (InputStream in = getClass().getResourceAsStream("/crates/basica.yml")) {
            if (in != null) {
                Files.copy(in, file.toPath());
            } else {
                writeMinimalExample(file);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao criar crate exemplo", e);
        }
    }

    private void writeMinimalExample(File file) {
        String content = """
                id: basica
                display:
                  name: "<gradient:#FFD700:#FFA500>Crate Basica"
                  engine: PHYSICAL_CHEST
                  vanilla:
                    item: CHEST
                    scale: [ 1.0, 1.0, 1.0 ]
                price:
                  currency: gold
                  amount: 0
                rewards:
                  diamante:
                    id: diamante
                    type: ITEM
                    item: DIAMOND
                    amount: 5
                    chance: 20.0
                    display-name: "5 Diamantes"
                  esmeralda:
                    id: esmeralda
                    type: ITEM
                    item: EMERALD
                    amount: 10
                    chance: 20.0
                    display-name: "10 Esmeraldas"
                  ouro:
                    id: ouro
                    type: ITEM
                    item: GOLD_INGOT
                    amount: 32
                    chance: 25.0
                    display-name: "32 Barras de Ouro"
                    win-limit: 3
                    win-limit-cooldown: 3600
                  moedas:
                    id: moedas
                    type: MONEY
                    currency: gold
                    amount: 500
                    chance: 20.0
                    display-name: "500 Moedas"
                  jackpot:
                    id: jackpot
                    type: COMMAND
                    command: "say %player% ganhou o jackpot na Crate Basica!"
                    chance: 15.0
                    display-name: "Jackpot"
                    global-win-limit: 5
                    broadcast: true
                """;
        try {
            Files.writeString(file.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao escrever crate exemplo", e);
        }
    }
}
