package com.alkacode.crates.repository;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keys virtuais por jogador (tabela alkacrates_keys). */
public final class VirtualKeyRepository extends AbstractRepository {

    public VirtualKeyRepository(DatabaseProvider db) {
        super(db);
    }

    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS alkacrates_keys ("
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "crate_id VARCHAR(64) NOT NULL, "
                + "amount INT NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (player_uuid, crate_id))";
        execute(sql, ps -> {});
    }

    /** Carrega todas as keys virtuais de um jogador de uma vez (usado no join, ver VirtualKeyManager). */
    public Map<String, Integer> loadAll(UUID player) throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT crate_id, amount FROM alkacrates_keys WHERE player_uuid = ?";
        try (var conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("crate_id"), rs.getInt("amount"));
                }
            }
        }
        return result;
    }

    public int getKeys(UUID player, String crateId) throws SQLException {
        String sql = "SELECT amount FROM alkacrates_keys WHERE player_uuid = ? AND crate_id = ?";
        return dbQuery(sql, ps -> {
            ps.setString(1, player.toString());
            ps.setString(2, crateId);
        });
    }

    private int dbQuery(String sql, SQLConsumer<PreparedStatement> binder) throws SQLException {
        try (var conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.accept(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("amount") : 0;
            }
        }
    }

    public void addKeys(UUID player, String crateId, int amount) throws SQLException {
        // NOTA: nao usa upsert() do Core porque ele faz `SET amount = excluded.amount`
        // (sobrescreve). Aqui precisamos SOMAR o saldo existente.
        String sql = db.isSQLite()
                ? "INSERT INTO alkacrates_keys (player_uuid, crate_id, amount) VALUES (?,?,?) "
                    + "ON CONFLICT(player_uuid, crate_id) DO UPDATE SET amount = amount + excluded.amount"
                : "INSERT INTO alkacrates_keys (player_uuid, crate_id, amount) VALUES (?,?,?) "
                    + "ON DUPLICATE KEY UPDATE amount = amount + VALUES(amount)";
        execute(sql, ps -> {
            ps.setString(1, player.toString());
            ps.setString(2, crateId);
            ps.setInt(3, amount);
        });
    }

    public void removeKeys(UUID player, String crateId, int amount) throws SQLException {
        int current = getKeys(player, crateId);
        int remaining = Math.max(0, current - amount);
        if (remaining <= 0) {
            String sql = "DELETE FROM alkacrates_keys WHERE player_uuid = ? AND crate_id = ?";
            execute(sql, ps -> {
                ps.setString(1, player.toString());
                ps.setString(2, crateId);
            });
        } else {
            String sql = "UPDATE alkacrates_keys SET amount = ? WHERE player_uuid = ? AND crate_id = ?";
            execute(sql, ps -> {
                ps.setInt(1, remaining);
                ps.setString(2, player.toString());
                ps.setString(3, crateId);
            });
        }
    }
}
