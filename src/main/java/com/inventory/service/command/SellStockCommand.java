package com.inventory.service.command;

import com.inventory.dao.StockItemDao;
import com.inventory.dao.StockMovementDao;
import com.inventory.model.StockItem;
import com.inventory.model.StockMovement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

        // Snapshot the product's cost basis *at the moment of sale*, on the same
        // connection/transaction. This is what makes historical COGS/profit
        // reporting immune to later edits to product.unit_cost (see
        // docs/05-architecture.md, "historical cost-basis snapshot").
        double costBasisAtSaleTime = currentUnitCost(conn);

        stockItemDao.adjustQuantity(productId, warehouseId, -quantity, conn);

        String customer = (customerName == null || customerName.isBlank()) ? "Walk-in Customer" : customerName.trim();
        String reason = String.format("Sold %d units to %s @ $%.2f/unit", quantity, customer, unitPrice);

        movementDao.insert(new StockMovement(productId, warehouseId, null, "OUT", quantity,
                unitPrice, costBasisAtSaleTime, reason), conn);
    }

    private double currentUnitCost(Connection conn) throws SQLException {
        String sql = "SELECT unit_cost FROM product WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("unit_cost") : 0.0;
            }
        }
    }
}
