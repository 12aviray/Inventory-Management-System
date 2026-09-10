package com.inventory.dao;

import com.inventory.model.StockMovement;
import com.inventory.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockMovementDao {

    public void insert(StockMovement m, Connection conn) throws SQLException {
        String sql = "INSERT INTO stock_movement (product_id, warehouse_id, po_id, movement_type, quantity, unit_price, unit_cost, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, m.getProductId());
            ps.setInt(2, m.getWarehouseId());
            if (m.getPoId() != null) {
                ps.setInt(3, m.getPoId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, m.getMovementType());
            ps.setInt(5, m.getQuantity());
            ps.setDouble(6, m.getUnitPrice());
            ps.setDouble(7, m.getUnitCost());
            ps.setString(8, m.getReason());
            ps.executeUpdate();
        }
    }

    /** Filterable audit/history report. Any parameter may be null to mean "no filter". */
    public List<StockMovement> search(Integer productId, Integer warehouseId, Long fromEpoch, Long toEpoch) {
        StringBuilder sql = new StringBuilder("""
            SELECT sm.*, p.name AS product_name, w.name AS warehouse_name
            FROM stock_movement sm
            JOIN product p ON p.product_id = sm.product_id
            JOIN warehouse w ON w.warehouse_id = sm.warehouse_id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        if (productId != null) { sql.append(" AND sm.product_id = ?"); params.add(productId); }
        if (warehouseId != null) { sql.append(" AND sm.warehouse_id = ?"); params.add(warehouseId); }
        if (fromEpoch != null) { sql.append(" AND sm.created_at >= ?"); params.add(fromEpoch); }
        if (toEpoch != null) { sql.append(" AND sm.created_at <= ?"); params.add(toEpoch); }
        sql.append(" ORDER BY sm.created_at DESC");

        List<StockMovement> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockMovement m = new StockMovement();
                    m.setMovementId(rs.getInt("movement_id"));
                    m.setProductId(rs.getInt("product_id"));
                    m.setWarehouseId(rs.getInt("warehouse_id"));
                    int po = rs.getInt("po_id");
                    m.setPoId(rs.wasNull() ? null : po);
                    m.setMovementType(rs.getString("movement_type"));
                    m.setQuantity(rs.getInt("quantity"));
                    m.setUnitPrice(rs.getDouble("unit_price"));
                    m.setUnitCost(rs.getDouble("unit_cost"));
                    m.setReason(rs.getString("reason"));
                    m.setCreatedAt(rs.getLong("created_at"));
                    m.setProductName(rs.getString("product_name"));
                    m.setWarehouseName(rs.getString("warehouse_name"));
                    results.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search stock movements", e);
        }
        return results;
    }
}
