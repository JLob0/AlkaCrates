package com.alkacode.crates.repository;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Contador de aberturas por jogador/crate para guaranteed reward (pity). */
public final class PityRepository extends AbstractRepository {

    public PityRepository(DatabaseProvider db) {
        super(db);
    }

    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS alkacrates_pity ("
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "crate_id VARCHAR(64) NOT NULL, "
                + "opens INT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (player_uuid, crate_id))";
        execute(sql, ps -> {});
    }

    public int getOpens(UUID player, String crateId) throws SQLException {
        String sql = "SELECT opens FROM alkacrates_pity WHERE player_uuid = ? AND crate_id = ?";
        try (var conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            ps.setString(2, crateId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("opens") : 0;
            }
        }
    }

    public void increment(UUID player, String crateId) throws SQLException {
        // NOTA: nao usa upsert() do Core porque ele faz `SET opens = excluded.opens`
        // (sobrescreve com 1). Aqui precisamos INCREMENTAR o contador.
        String sql = db.isSQLite()
                ? "INSERT INTO alkacrates_pity (player_uuid, crate_id, opens) VALUES (?,?,1) "
                    + "ON CONFLICT(player_uuid, crate_id) DO UPDATE SET opens = opens + 1"
                : "INSERT INTO alkacrates_pity (player_uuid, crate_id, opens) VALUES (?,?,1) "
                    + "ON DUPLICATE KEY UPDATE opens = opens + 1";
        execute(sql, ps -> {
            ps.setString(1, player.toString());
            ps.setString(2, crateId);
        });
    }

    public void reset(UUID player, String crateId) throws SQLException {
        String sql = "DELETE FROM alkacrates_pity WHERE player_uuid = ? AND crate_id = ?";
        execute(sql, ps -> {
            ps.setString(1, player.toString());
            ps.setString(2, crateId);
        });
    }
}
