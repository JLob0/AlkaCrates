package com.alkacode.crates.repository;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Contagem de vitorias por reward - usada para win-limit (por jogador) e
 * global-win-limit (todos os jogadores juntos, ex: jackpot com estoque limitado).
 */
public final class RewardWinRepository extends AbstractRepository {

    public RewardWinRepository(DatabaseProvider db) {
        super(db);
    }

    public void createTable() throws SQLException {
        execute("CREATE TABLE IF NOT EXISTS alkacrates_reward_wins ("
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "crate_id VARCHAR(64) NOT NULL, "
                + "reward_id VARCHAR(64) NOT NULL, "
                + "wins INT NOT NULL DEFAULT 0, "
                + "last_win_epoch BIGINT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (player_uuid, crate_id, reward_id))", ps -> {});
        execute("CREATE TABLE IF NOT EXISTS alkacrates_reward_global_wins ("
                + "crate_id VARCHAR(64) NOT NULL, "
                + "reward_id VARCHAR(64) NOT NULL, "
                + "wins INT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (crate_id, reward_id))", ps -> {});
    }

    public record WinRecord(int wins, long lastWinEpochSeconds) {
        public static final WinRecord EMPTY = new WinRecord(0, 0);
    }

    /** Chave composta "crateId:rewardId" usada nos maps de cache. */
    public static String key(String crateId, String rewardId) {
        return crateId + ':' + rewardId;
    }

    public Map<String, WinRecord> loadAll(UUID player) throws SQLException {
        Map<String, WinRecord> result = new HashMap<>();
        String sql = "SELECT crate_id, reward_id, wins, last_win_epoch FROM alkacrates_reward_wins WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(key(rs.getString("crate_id"), rs.getString("reward_id")),
                            new WinRecord(rs.getInt("wins"), rs.getLong("last_win_epoch")));
                }
            }
        }
        return result;
    }

    /** Carga unica no enable - a tabela de rewards e pequena (catalogo de crates, nao jogadores). */
    public Map<String, Integer> loadAllGlobal() throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT crate_id, reward_id, wins FROM alkacrates_reward_global_wins")) {
            while (rs.next()) {
                result.put(key(rs.getString("crate_id"), rs.getString("reward_id")), rs.getInt("wins"));
            }
        }
        return result;
    }

    public void incrementPlayer(UUID player, String crateId, String rewardId, long nowEpochSeconds) throws SQLException {
        String sql = db.isSQLite()
                ? "INSERT INTO alkacrates_reward_wins (player_uuid, crate_id, reward_id, wins, last_win_epoch) VALUES (?,?,?,1,?) "
                    + "ON CONFLICT(player_uuid, crate_id, reward_id) DO UPDATE SET wins = wins + 1, last_win_epoch = excluded.last_win_epoch"
                : "INSERT INTO alkacrates_reward_wins (player_uuid, crate_id, reward_id, wins, last_win_epoch) VALUES (?,?,?,1,?) "
                    + "ON DUPLICATE KEY UPDATE wins = wins + 1, last_win_epoch = VALUES(last_win_epoch)";
        execute(sql, ps -> {
            ps.setString(1, player.toString());
            ps.setString(2, crateId);
            ps.setString(3, rewardId);
            ps.setLong(4, nowEpochSeconds);
        });
    }

    public void incrementGlobal(String crateId, String rewardId) throws SQLException {
        String sql = db.isSQLite()
                ? "INSERT INTO alkacrates_reward_global_wins (crate_id, reward_id, wins) VALUES (?,?,1) "
                    + "ON CONFLICT(crate_id, reward_id) DO UPDATE SET wins = wins + 1"
                : "INSERT INTO alkacrates_reward_global_wins (crate_id, reward_id, wins) VALUES (?,?,1) "
                    + "ON DUPLICATE KEY UPDATE wins = wins + 1";
        execute(sql, ps -> {
            ps.setString(1, crateId);
            ps.setString(2, rewardId);
        });
    }
}
