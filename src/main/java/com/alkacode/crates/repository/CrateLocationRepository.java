package com.alkacode.crates.repository;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import com.alkacode.crates.crate.model.CrateLocation;
import org.bukkit.Location;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Persiste onde cada crate fisica esta no mundo (alkacrates_locations) + a tag unica (AC-###) de cada uma. */
public final class CrateLocationRepository extends AbstractRepository {

    public CrateLocationRepository(DatabaseProvider db) {
        super(db);
    }

    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS alkacrates_locations ("
                + "crate_id VARCHAR(64) NOT NULL, "
                + "world VARCHAR(64) NOT NULL, "
                + "x DOUBLE NOT NULL, "
                + "y DOUBLE NOT NULL, "
                + "z DOUBLE NOT NULL, "
                + "yaw FLOAT NOT NULL, "
                + "pitch FLOAT NOT NULL, "
                + "tag VARCHAR(32), "
                + "PRIMARY KEY (world, x, y, z))";
        execute(sql, ps -> {});
        ensureTagColumn();
        execute("CREATE TABLE IF NOT EXISTS alkacrates_tag_counter ("
                + "id INTEGER PRIMARY KEY, "
                + "next_value INTEGER NOT NULL)", ps -> {});
    }

    /** Migracao: `CREATE TABLE IF NOT EXISTS` nao adiciona coluna em tabela ja existente de antes da tag existir. */
    private void ensureTagColumn() {
        try {
            execute("ALTER TABLE alkacrates_locations ADD COLUMN tag VARCHAR(32)", ps -> {});
        } catch (SQLException ignored) {
            // coluna ja existe - tudo bem, e o caso normal em instalacao nova ou apos a 1a migracao.
        }
    }

    /** Gera a proxima tag unica (AC-001, AC-002...) de forma atomica e persistente. */
    public String nextTag() throws SQLException {
        int value = inTransaction(conn -> {
            int current;
            try (PreparedStatement select = conn.prepareStatement(
                    "SELECT next_value FROM alkacrates_tag_counter WHERE id = 1")) {
                try (ResultSet rs = select.executeQuery()) {
                    current = rs.next() ? rs.getInt("next_value") : 1;
                }
            }
            String upsert = db.isSQLite()
                    ? "INSERT INTO alkacrates_tag_counter (id, next_value) VALUES (1, ?) "
                        + "ON CONFLICT(id) DO UPDATE SET next_value = excluded.next_value"
                    : "INSERT INTO alkacrates_tag_counter (id, next_value) VALUES (1, ?) "
                        + "ON DUPLICATE KEY UPDATE next_value = VALUES(next_value)";
            try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                ps.setInt(1, current + 1);
                ps.executeUpdate();
            }
            return current;
        });
        return String.format("AC-%03d", value);
    }

    public void save(CrateLocation crateLocation) throws SQLException {
        String sql = "INSERT INTO alkacrates_locations (crate_id, world, x, y, z, yaw, pitch, tag) VALUES (?,?,?,?,?,?,?,?)";
        execute(sql, ps -> {
            ps.setString(1, crateLocation.getCrateId());
            ps.setString(2, crateLocation.getWorld());
            ps.setDouble(3, crateLocation.getX());
            ps.setDouble(4, crateLocation.getY());
            ps.setDouble(5, crateLocation.getZ());
            ps.setFloat(6, crateLocation.getYaw());
            ps.setFloat(7, crateLocation.getPitch());
            ps.setString(8, crateLocation.getTag());
        });
    }

    /** Backfill de tag pra crate colocada antes dessa versao (achada sem tag no loadAll). */
    public void updateTag(Location location, String tag) throws SQLException {
        String sql = "UPDATE alkacrates_locations SET tag = ? WHERE world = ? AND x = ? AND y = ? AND z = ?";
        execute(sql, ps -> {
            ps.setString(1, tag);
            ps.setString(2, location.getWorld().getName());
            ps.setDouble(3, location.getX());
            ps.setDouble(4, location.getY());
            ps.setDouble(5, location.getZ());
        });
    }

    public void delete(Location location) throws SQLException {
        String sql = "DELETE FROM alkacrates_locations WHERE world = ? AND x = ? AND y = ? AND z = ?";
        execute(sql, ps -> {
            ps.setString(1, location.getWorld().getName());
            ps.setDouble(2, location.getX());
            ps.setDouble(3, location.getY());
            ps.setDouble(4, location.getZ());
        });
    }

    public void deleteByTag(String tag) throws SQLException {
        execute("DELETE FROM alkacrates_locations WHERE tag = ?", ps -> ps.setString(1, tag));
    }

    /** Usado ao deletar uma crate pelo editor - sem isso, sobrava localizacao apontando pra um id inexistente. */
    public void deleteByCrateId(String crateId) throws SQLException {
        String sql = "DELETE FROM alkacrates_locations WHERE crate_id = ?";
        execute(sql, ps -> ps.setString(1, crateId));
    }

    public List<CrateLocation> findAll() throws SQLException {
        String sql = "SELECT crate_id, world, x, y, z, yaw, pitch, tag FROM alkacrates_locations";
        List<CrateLocation> result = new ArrayList<>();
        try (var conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new CrateLocation(
                        rs.getString("crate_id"),
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch"),
                        rs.getString("tag")));
            }
        }
        return result;
    }
}
