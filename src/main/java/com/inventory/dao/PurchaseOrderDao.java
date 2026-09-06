package com.inventory.dao;

import com.inventory.model.PurchaseOrder;
import com.inventory.model.PurchaseOrderLine;
import com.inventory.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PurchaseOrderDao {

    public PurchaseOrder insert(PurchaseOrder po, Connection conn) throws SQLException {
        String sql = "INSERT INTO purchase_order (supplier_id, status, expected_date, notes) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, po.getSupplierId());
            ps.setString(2, po.getStatus());
            ps.setLong(3, po.getExpectedDate());
            ps.setString(4, po.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) po.setPoId(keys.getInt(1));
            }
        }
        String lineSql = "INSERT INTO purchase_order_line (po_id, product_id, quantity_ordered, unit_cost) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(lineSql)) {
            for (PurchaseOrderLine line : po.getLines()) {
                ps.setInt(1, po.getPoId());
                ps.setInt(2, line.getProductId());
                ps.setInt(3, line.getQuantityOrdered());
                ps.setDouble(4, line.getUnitCost());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return po;
    }

    public Optional<PurchaseOrder> findById(int poId) {
        String sql = """
            SELECT po.*, s.name AS supplier_name
            FROM purchase_order po JOIN supplier s ON s.supplier_id = po.supplier_id
            WHERE po.po_id = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, poId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                PurchaseOrder po = mapHeader(rs);
                loadLines(po, conn);
                return Optional.of(po);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch purchase order " + poId, e);
        }
    }

    public List<PurchaseOrder> findAll() {
        String sql = """
            SELECT po.*, s.name AS supplier_name
            FROM purchase_order po JOIN supplier s ON s.supplier_id = po.supplier_id
            ORDER BY po.created_at DESC
            """;
        List<PurchaseOrder> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                PurchaseOrder po = mapHeader(rs);
                loadLines(po, conn);
                results.add(po);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch purchase orders", e);
        }
        return results;
    }

    public void updateStatus(int poId, String newStatus, Connection conn) throws SQLException {
        String sql = "UPDATE purchase_order SET status=? WHERE po_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, poId);
            ps.executeUpdate();
        }
    }

    public void updateLineReceivedQuantity(int poLineId, int newReceivedQty, Connection conn) throws SQLException {
        String sql = "UPDATE purchase_order_line SET quantity_received=? WHERE po_line_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newReceivedQty);
            ps.setInt(2, poLineId);
            ps.executeUpdate();
        }
    }

    private PurchaseOrder mapHeader(ResultSet rs) throws SQLException {
        PurchaseOrder po = new PurchaseOrder();
        po.setPoId(rs.getInt("po_id"));
        po.setSupplierId(rs.getInt("supplier_id"));
        po.setSupplierName(rs.getString("supplier_name"));
        po.setStatus(rs.getString("status"));
        po.setExpectedDate(rs.getLong("expected_date"));
        po.setNotes(rs.getString("notes"));
        return po;
    }

    private void loadLines(PurchaseOrder po, Connection conn) throws SQLException {
        String sql = """
            SELECT pol.*, p.name AS product_name
            FROM purchase_order_line pol JOIN product p ON p.product_id = pol.product_id
            WHERE pol.po_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, po.getPoId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PurchaseOrderLine line = new PurchaseOrderLine();
                    line.setPoLineId(rs.getInt("po_line_id"));
                    line.setPoId(po.getPoId());
                    line.setProductId(rs.getInt("product_id"));
                    line.setProductName(rs.getString("product_name"));
                    line.setQuantityOrdered(rs.getInt("quantity_ordered"));
                    line.setQuantityReceived(rs.getInt("quantity_received"));
                    line.setUnitCost(rs.getDouble("unit_cost"));
                    po.addLine(line);
                }
            }
        }
    }
}
