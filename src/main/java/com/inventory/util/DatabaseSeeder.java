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

            if (isEmpty(st, "purchase_order")) {
                st.executeUpdate("""
                    INSERT INTO purchase_order (po_id, supplier_id, status, expected_date, notes) VALUES
                    (1, 1, 'SENT', strftime('%s','now', '+5 days'), 'Urgent restocking for Q3 electronics'),
                    (2, 2, 'PARTIALLY_RECEIVED', strftime('%s','now', '+2 days'), 'Office chair & desk lamp replenishment')
                    """);

                st.executeUpdate("""
                    INSERT INTO purchase_order_line (po_line_id, po_id, product_id, quantity_ordered, quantity_received, unit_cost) VALUES
                    (1, 1, 1, 50, 0, 12.50),
                    (2, 1, 2, 100, 0, 4.25),
                    (3, 2, 3, 10, 5, 89.99),
                    (4, 2, 5, 20, 10, 24.75)
                    """);
            }

            if (isEmpty(st, "stock_movement")) {
                st.executeUpdate("""
                    INSERT INTO stock_movement (product_id, warehouse_id, po_id, movement_type, quantity, unit_price, reason) VALUES
                    (1, 1, NULL, 'IN', 20, 12.50, 'Initial Inventory Stock In'),
                    (2, 1, NULL, 'IN', 100, 4.25, 'Initial Inventory Stock In'),
                    (3, 1, 2, 'IN', 5, 89.99, 'Received against PO #2'),
                    (4, 2, NULL, 'IN', 250, 2.10, 'Initial Inventory Stock In'),
                    (5, 2, 2, 'IN', 10, 24.75, 'Received against PO #2'),
                    (1, 1, NULL, 'OUT', 5, 19.99, 'Sold 5 units to Apex Enterprises @ $19.99/unit'),
                    (2, 1, NULL, 'OUT', 20, 7.50, 'Sold 20 units to Byte Solutions @ $7.50/unit'),
                    (3, 1, NULL, 'OUT', 2, 139.99, 'Sold 2 units to Modern Tech Hub @ $139.99/unit'),
                    (4, 2, NULL, 'OUT', 50, 4.50, 'Sold 50 units to City Academy @ $4.50/unit'),
                    (5, 2, NULL, 'OUT', 6, 39.99, 'Sold 6 units to Creative Studios @ $39.99/unit')
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
