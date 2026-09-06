package com.inventory.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the schema (if needed) and loads sample data so the application
 * always has usable data on a fresh checkout/run, per the project requirement
 * that teams provide seeders.
 */
public final class DatabaseSeeder {

    private DatabaseSeeder() {
    }

    public static void run() {
        DatabaseManager.initializeSchema();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {

            if (isEmpty(st, "product")) {
                st.executeUpdate("""
                    INSERT INTO product (sku, name, category, unit_cost, reorder_threshold) VALUES
                    ('SKU-001', 'Wireless Mouse', 'Electronics', 12.50, 20),
                    ('SKU-002', 'USB-C Cable 1m', 'Electronics', 4.25, 50),
                    ('SKU-003', 'Office Chair', 'Furniture', 89.99, 5),
                    ('SKU-004', 'Notebook A5', 'Stationery', 2.10, 100),
                    ('SKU-005', 'Desk Lamp', 'Furniture', 24.75, 10)
                    """);
            }

            if (isEmpty(st, "warehouse")) {
                st.executeUpdate("""
                    INSERT INTO warehouse (name, location) VALUES
                    ('Main Warehouse', 'Dhaka'),
                    ('North Depot', 'Gazipur')
                    """);
            }

            if (isEmpty(st, "supplier")) {
                st.executeUpdate("""
                    INSERT INTO supplier (name, contact_email, phone) VALUES
                    ('TechSource Ltd.', 'sales@techsource.example', '01700000000'),
                    ('Furniture World', 'contact@furnitureworld.example', '01800000000')
                    """);
            }

            if (isEmpty(st, "stock_item")) {
                st.executeUpdate("""
                    INSERT INTO stock_item (product_id, warehouse_id, quantity) VALUES
                    (1, 1, 15),
                    (2, 1, 80),
                    (3, 1, 3),
                    (4, 2, 200),
                    (5, 2, 4)
                    """);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed database", e);
        }
    }

    private static boolean isEmpty(Statement st, String table) throws SQLException {
        var rs = st.executeQuery("SELECT COUNT(*) AS c FROM " + table);
        rs.next();
        boolean empty = rs.getInt("c") == 0;
        rs.close();
        return empty;
    }
}
