package com.inventory.dao;

import com.inventory.model.Supplier;
import com.inventory.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierDao {

    public List<Supplier> findAll() {
        String sql = "SELECT * FROM supplier ORDER BY name";
        List<Supplier> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                results.add(new Supplier(rs.getInt("supplier_id"), rs.getString("name"),
                        rs.getString("contact_email"), rs.getString("phone")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch suppliers", e);
        }
        return results;
    }

    public Supplier insert(Supplier s) {
        String sql = "INSERT INTO supplier (name, contact_email, phone) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactEmail());
            ps.setString(3, s.getPhone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setSupplierId(keys.getInt(1));
            }
            return s;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert supplier", e);
        }
    }
}
