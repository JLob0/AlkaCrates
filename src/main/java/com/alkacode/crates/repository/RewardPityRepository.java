package com.alkacode.crates.repository;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Contador de "tentativas sem ganhar" por jogador/crate/reward - usado pelo soft pity
 * (ver RewardPityManager). Diferente do PityRepository (esse ali e o pity "duro" por
 * crate inteira, contando ABERTURAS pra liberar o pool guaranteed); este aqui conta
 * FALHAS de uma reward especifica, pra alimentar o aumento gradual de chance dela.
 */
public final class RewardPityRepository extends AbstractRepository {

    public RewardPityRepository(DatabaseProvider db) {
        super(db);
    }

    public void createTable() throws SQLException {
        execute("CREATE TABLE IF NOT EXISTS alkacrates_reward_pity ("
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "crate_id VARCHAR(64) NOT NULL, "
                + "reward_id VARCHAR(64) NOT NULL, "
                + "attempts INT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (player_uuid, crate_id, reward_id))", ps -> {});
    }

    public static String key(String crateId, String rewardId) {
        return crateId + ':' + rewardId;
    }

    public Map<String, Integer> loadAll(UUID player) throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT crate_id, reward_id, attempts FROM alkacrates_reward_pity WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(key(rs.getString("crate_id"), rs.getString("reward_id")), rs.getInt("attempts"));
                }
            }
        }
        return result;
    }

    public void increment(UUID player, String crateId, String rewardId) throws SQLException {
        String sql = db.isSQLite()
                ? "INSERT INTO alkacrates_reward_pity (player_uuid, crate_id, reward_id, attempts) VALUES (?,?,?,1) "
                    + "ON CONFLICT(player_uuid, crate_id, reward_id) DO UPDATE SET attempts = attempts + 1"
                : "INSERT INTO alkacrates_reward_pity (player_uuid, crate_id, reward_id, attempts) VALUES (?,?,?,1) "
                    + "ON DUPLICATE KEY UPDATE attempts = attempts + 1";
        execute(sql, ps -> {
            ps.setString(1, player.toString());
            ps.setString(2, crateId);
            ps.setString(3, rewardId);
        });
    }

    public void reset(UUID player, String crateId, String rewardId) throws SQLException {
        String sql = "DELETE FROM alkacrates_reward_pity WHERE player_uuid = ? AND crate_id = ? AND reward_id = ?";
        execute(sql, ps -> {
            ps.setString(1, player.toString());
            ps.setString(2, crateId);
            ps.setString(3, rewardId);
        });
    }
}
