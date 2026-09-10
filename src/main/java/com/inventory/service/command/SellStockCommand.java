package com.inventory.service.command;

import com.inventory.dao.StockItemDao;
import com.inventory.dao.StockMovementDao;
import com.inventory.model.StockItem;
import com.inventory.model.StockMovement;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Command pattern implementation for customer sales and stock dispatch.
 * Atomically decreases on-hand warehouse quantity and records an 'OUT' stock movement.
 */
public class SellStockCommand implements StockCommand {

    private final StockItemDao stockItemDao = new StockItemDao();
    private final StockMovementDao movementDao = new StockMovementDao();

    private final int productId;
    private final int warehouseId;
    private final int quantity;
    private final String customerName;
    private final double unitPrice;

    public SellStockCommand(int productId, int warehouseId, int quantity, String customerName, double unitPrice) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.customerName = customerName;
        this.unitPrice = unitPrice;
    }

    @Override
    public void execute(Connection conn) throws SQLException {
        Optional<StockItem> source = stockItemDao.find(productId, warehouseId, conn);
        int available = source.map(StockItem::getQuantity).orElse(0);
        if (available < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock at warehouse: have " + available + ", requested " + quantity);
        }

        stockItemDao.adjustQuantity(productId, warehouseId, -quantity, conn);

        String customer = (customerName == null || customerName.isBlank()) ? "Walk-in Customer" : customerName.trim();
        String reason = String.format("Sold %d units to %s @ $%.2f/unit", quantity, customer, unitPrice);

        movementDao.insert(new StockMovement(productId, warehouseId, null, "OUT", quantity, unitPrice, reason), conn);
    }
}
