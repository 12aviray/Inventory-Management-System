package com.inventory.service;

import com.inventory.service.command.StockCommand;
import com.inventory.service.command.TransferStockCommand;
import com.inventory.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;

public class TransferService {

    public void transfer(int productId, int fromWarehouseId, int toWarehouseId, int quantity) {
        if (fromWarehouseId == toWarehouseId) {
            throw new IllegalArgumentException("Source and destination warehouse must differ");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be positive");
        }

        StockCommand command = new TransferStockCommand(productId, fromWarehouseId, toWarehouseId, quantity);
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
            throw new RuntimeException("Failed to transfer stock", e);
        }
    }
}
