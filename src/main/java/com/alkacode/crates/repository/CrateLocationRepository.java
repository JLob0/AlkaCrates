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

/** Persiste onde cada crate fisica esta no mundo (alkacrates_locations). */
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
                + "PRIMARY KEY (world, x, y, z))";
        execute(sql, ps -> {});
    }

    public void save(CrateLocation crateLocation) throws SQLException {
        String sql = "INSERT INTO alkacrates_locations (crate_id, world, x, y, z, yaw, pitch) VALUES (?,?,?,?,?,?,?)";
        execute(sql, ps -> {
            ps.setString(1, crateLocation.getCrateId());
            ps.setString(2, crateLocation.getWorld());
            ps.setDouble(3, crateLocation.getX());
            ps.setDouble(4, crateLocation.getY());
            ps.setDouble(5, crateLocation.getZ());
            ps.setFloat(6, crateLocation.getYaw());
            ps.setFloat(7, crateLocation.getPitch());
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

    public List<CrateLocation> findAll() throws SQLException {
        String sql = "SELECT crate_id, world, x, y, z, yaw, pitch FROM alkacrates_locations";
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
                        rs.getFloat("pitch")));
            }
        }
        return result;
    }
}
