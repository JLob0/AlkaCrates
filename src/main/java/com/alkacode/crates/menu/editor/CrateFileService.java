package com.alkacode.crates.menu.editor;

import com.alkacode.crates.AlkaCrates;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Le/escreve os YAML de crates/*.yml diretamente (sem passar pelos modelos imutaveis
 * Crate/Reward) pro editor em GUI. Cada tela do editor carrega o arquivo do disco,
 * muda um campo, salva e chama AlkaCrates#reloadEverything() - write-through, sem
 * estado "pendente" pra nao arriscar perder edicao se o admin fechar o inventario
 * no meio do caminho.
 */
public final class CrateFileService {

    private final AlkaCrates plugin;

    public CrateFileService(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    private File folder() {
        File folder = new File(plugin.getDataFolder(), plugin.getConfig().getString("crates-folder", "crates"));
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    public File fileFor(String crateId) {
        return new File(folder(), crateId + ".yml");
    }

    public boolean exists(String crateId) {
        return fileFor(crateId).exists();
    }

    public YamlConfiguration load(String crateId) {
        return YamlConfiguration.loadConfiguration(fileFor(crateId));
    }

    public void save(String crateId, YamlConfiguration config) {
        try {
            config.save(fileFor(crateId));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao salvar crate " + crateId, e);
        }
    }

    /** Salva e recarrega tudo (config.yml + crates/*.yml + crates ja colocadas no mundo). */
    public void saveAndReload(String crateId, YamlConfiguration config) {
        save(crateId, config);
        plugin.reloadEverything();
    }

    /** Normaliza um id digitado no chat (minusculo, so [a-z0-9_]) e retorna null se invalido/ja existente. */
    public String validateNewId(String raw) {
        if (raw == null) {
            return null;
        }
        String id = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (id.isEmpty() || !id.matches("[a-z0-9_]+") || exists(id)) {
            return null;
        }
        return id;
    }

    public YamlConfiguration createTemplate(String id) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("id", id);
        config.set("display.name", "<gradient:#FFD700:#FFA500>" + id);
        config.set("display.engine", "PHYSICAL_CHEST");
        config.set("display.vanilla.item", "CHEST");
        config.set("display.vanilla.scale", List.of(1.0, 1.0, 1.0));
        config.set("price.currency", "gold");
        config.set("price.amount", 0.0);
        return config;
    }

    /** Deleta o YAML e limpa localizacoes persistidas apontando pra esse id (nao chama reloadEverything sozinho). */
    public void delete(String crateId) {
        fileFor(crateId).delete();
        try {
            plugin.getCrateLocationRepository().deleteByCrateId(crateId);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao limpar localizacoes da crate deletada " + crateId, e);
        }
    }

    public List<String> rewardIds(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("rewards");
        return section != null ? new ArrayList<>(section.getKeys(false)) : new ArrayList<>();
    }

    /** Cria uma reward nova com defaults sensatos e devolve o id gerado. */
    public String addReward(YamlConfiguration config) {
        String id = "recompensa_" + UUID.randomUUID().toString().substring(0, 6);
        String path = "rewards." + id;
        config.set(path + ".id", id);
        config.set(path + ".type", "ITEM");
        config.set(path + ".item", "STONE");
        config.set(path + ".amount", 1);
        config.set(path + ".chance", 10.0);
        config.set(path + ".display-name", "Nova recompensa");
        return id;
    }

    public void removeReward(YamlConfiguration config, String rewardId) {
        config.set("rewards." + rewardId, null);
    }
}
