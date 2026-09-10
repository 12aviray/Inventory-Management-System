package com.inventory.dao;

import com.inventory.model.Warehouse;
import com.inventory.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WarehouseDao {

    public List<Warehouse> findAll() {
        String sql = "SELECT * FROM warehouse ORDER BY name";
        List<Warehouse> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch warehouses", e);
        }
        return results;
    }

    public Optional<Warehouse> findById(int warehouseId) {
        String sql = "SELECT * FROM warehouse WHERE warehouse_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, warehouseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch warehouse " + warehouseId, e);
        }
        return Optional.empty();
    }

    public Warehouse insert(Warehouse w) {
        String sql = "INSERT INTO warehouse (name, location) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, w.getName());
            ps.setString(2, w.getLocation());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) w.setWarehouseId(keys.getInt(1));
            }
            return w;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert warehouse", e);
        }
    }

    public void update(Warehouse w) {
        String sql = "UPDATE warehouse SET name=?, location=? WHERE warehouse_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, w.getName());
            ps.setString(2, w.getLocation());
            ps.setInt(3, w.getWarehouseId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update warehouse " + w.getWarehouseId(), e);
        }
    }

    public void delete(int warehouseId) {
        String sql = "DELETE FROM warehouse WHERE warehouse_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, warehouseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete warehouse " + warehouseId, e);
        }
    }

    private Warehouse mapRow(ResultSet rs) throws SQLException {
        return new Warehouse(rs.getInt("warehouse_id"), rs.getString("name"), rs.getString("location"));
    }
}
