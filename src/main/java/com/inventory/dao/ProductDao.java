package com.inventory.dao;

import com.inventory.model.Product;
import com.inventory.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for Product. Isolates all JDBC/SQL details from the
 * service (business logic) layer — the Repository/DAO separation required
 * by the project spec.
 */
public class ProductDao {

    public List<Product> findAll() {
        String sql = "SELECT * FROM product ORDER BY name";
        List<Product> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch products", e);
        }
        return results;
    }

    public Optional<Product> findById(int productId) {
        String sql = "SELECT * FROM product WHERE product_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch product " + productId, e);
        }
        return Optional.empty();
    }

    public Product insert(Product p) {
        String sql = "INSERT INTO product (sku, name, category, unit_cost, reorder_threshold) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getSku());
            ps.setString(2, p.getName());
            ps.setString(3, p.getCategory());
            ps.setDouble(4, p.getUnitCost());
            ps.setInt(5, p.getReorderThreshold());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setProductId(keys.getInt(1));
                }
            }
            return p;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert product", e);
        }
    }

    public void update(Product p) {
        String sql = "UPDATE product SET sku=?, name=?, category=?, unit_cost=?, reorder_threshold=? WHERE product_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getSku());
            ps.setString(2, p.getName());
            ps.setString(3, p.getCategory());
            ps.setDouble(4, p.getUnitCost());
            ps.setInt(5, p.getReorderThreshold());
            ps.setInt(6, p.getProductId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update product " + p.getProductId(), e);
        }
    }

    public void delete(int productId) {
        String sql = "DELETE FROM product WHERE product_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete product " + productId, e);
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("product_id"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getDouble("unit_cost"),
                rs.getInt("reorder_threshold")
        );
    }
}
