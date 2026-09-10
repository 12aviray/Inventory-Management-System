package com.inventory.dao;

import com.inventory.model.StockItem;
import com.inventory.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StockItemDao {

    /** Joined view used by low-stock detection and warehouse stock screens. */
    public List<StockItem> findAllWithDetails() {
        String sql = """
            SELECT si.stock_item_id, si.product_id, si.warehouse_id, si.quantity,
                   p.name AS product_name, p.reorder_threshold,
                   w.name AS warehouse_name
            FROM stock_item si
            JOIN product p ON p.product_id = si.product_id
            JOIN warehouse w ON w.warehouse_id = si.warehouse_id
            ORDER BY w.name, p.name
            """;
        List<StockItem> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                StockItem item = new StockItem(
                        rs.getInt("stock_item_id"), rs.getInt("product_id"),
                        rs.getInt("warehouse_id"), rs.getInt("quantity"));
                item.setProductName(rs.getString("product_name"));
                item.setWarehouseName(rs.getString("warehouse_name"));
                item.setReorderThreshold(rs.getInt("reorder_threshold"));
                results.add(item);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch stock items", e);
        }
        return results;
    }

    public Optional<StockItem> find(int productId, int warehouseId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return find(productId, warehouseId, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch stock item", e);
        }
    }

    public Optional<StockItem> find(int productId, int warehouseId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM stock_item WHERE product_id=? AND warehouse_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, warehouseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new StockItem(rs.getInt("stock_item_id"), productId,
                            warehouseId, rs.getInt("quantity")));
                }
            }
        }
        return Optional.empty();
    }

    /** Upserts the quantity for a product/warehouse pair (used by movement processing). */
    public void adjustQuantity(int productId, int warehouseId, int delta, Connection conn) throws SQLException {
        Optional<StockItem> existing = find(productId, warehouseId, conn);
        if (existing.isPresent()) {
            String sql = "UPDATE stock_item SET quantity = quantity + ?, updated_at = strftime('%s','now') WHERE product_id=? AND warehouse_id=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, delta);
                ps.setInt(2, productId);
                ps.setInt(3, warehouseId);
                ps.executeUpdate();
            }
        } else {
            if (delta < 0) {
                throw new IllegalStateException("Cannot reduce stock that does not exist for product " + productId);
            }
            String sql = "INSERT INTO stock_item (product_id, warehouse_id, quantity) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, productId);
                ps.setInt(2, warehouseId);
                ps.setInt(3, delta);
                ps.executeUpdate();
            }
        }
    }
}
