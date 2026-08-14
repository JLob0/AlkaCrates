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
                  engine: VANILLA
                  vanilla:
                    item: DIAMOND_BLOCK
                    scale: [ 0.8, 0.8, 0.8 ]
                price:
                  currency: coins
                  amount: 0
                animation:
                  idle:
                    loop: true
                    keyframes:
                      - time: 0.0
                        transform:
                          offset: [ 0, 0, 0 ]
                          rotation: [ 0, 0, 0 ]
                          scale: [ 1.0, 1.0, 1.0 ]
                      - time: 2.0
                        transform:
                          offset: [ 0, 0.3, 0 ]
                          rotation: [ 0, 180, 0 ]
                          scale: [ 1.1, 1.1, 1.1 ]
                        easing: EASE_IN_OUT
                      - time: 4.0
                        transform:
                          offset: [ 0, 0, 0 ]
                          rotation: [ 0, 360, 0 ]
                          scale: [ 1.0, 1.0, 1.0 ]
                        easing: EASE_IN_OUT
                rewards:
                  diamante:
                    id: diamante
                    type: ITEM
                    item: DIAMOND
                    amount: 1
                    chance: 50.0
                    display-name: "Diamante"
                  moedas:
                    id: moedas
                    type: MONEY
                    currency: coins
                    amount: 100
                    chance: 30.0
                    display-name: "100 Moedas"
                  comando:
                    id: comando
                    type: COMMAND
                    command: "say %player% ganhou na crate"
                    chance: 20.0
                    display-name: "Comando Surpresa"
                """;
        try {
            Files.writeString(file.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao escrever crate exemplo", e);
        }
    }
}
