package com.inventory.service;

import com.inventory.service.command.SellStockCommand;
import com.inventory.service.command.StockCommand;
import com.inventory.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Service for processing customer sales and inventory dispatches.
 * Executes SellStockCommand within atomic database transactions.
 */
public class SalesService {

    public static class SaleItem {
        private final int productId;
        private final int quantity;
        private final double unitPrice;

        public SaleItem(int productId, int quantity, double unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public int getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
    }

    public void processSale(int productId, int warehouseId, int quantity, String customerName, double unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Sale quantity must be greater than 0.");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative.");
        }

        StockCommand command = new SellStockCommand(productId, warehouseId, quantity, customerName, unitPrice);

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                command.execute(conn);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to complete sale transaction", e);
        }
    }

    public void processBatchSale(List<SaleItem> items, int warehouseId, String customerName) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty.");
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (SaleItem item : items) {
                    if (item.getQuantity() <= 0) {
                        throw new IllegalArgumentException("Item quantity must be greater than 0.");
                    }
                    StockCommand cmd = new SellStockCommand(
                            item.getProductId(), warehouseId, item.getQuantity(), customerName, item.getUnitPrice());
                    cmd.execute(conn);
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to complete batch sale transaction", e);
        }
    }
}
