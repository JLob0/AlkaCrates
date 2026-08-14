package com.alkacode.crates.repository;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Log de aberturas (monetizacao/analise). */
public final class CrateLogRepository extends AbstractRepository {

    public CrateLogRepository(DatabaseProvider db) {
        super(db);
    }

    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS alkacrates_log ("
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "player_name VARCHAR(36) NOT NULL, "
                + "crate_id VARCHAR(64) NOT NULL, "
                + "reward_id VARCHAR(64) NOT NULL, "
                + "opened_at BIGINT NOT NULL)";
        execute(sql, ps -> {});
    }

    public void log(String playerUuid, String playerName, String crateId, String rewardId) throws SQLException {
        String sql = "INSERT INTO alkacrates_log (player_uuid, player_name, crate_id, reward_id, opened_at) VALUES (?,?,?,?,?)";
        execute(sql, ps -> {
            ps.setString(1, playerUuid);
            ps.setString(2, playerName);
            ps.setString(3, crateId);
            ps.setString(4, rewardId);
            ps.setLong(5, System.currentTimeMillis());
        });
    }

    public int countLogs() throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM alkacrates_log";
        try (var conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("c") : 0;
        }
    }
}
