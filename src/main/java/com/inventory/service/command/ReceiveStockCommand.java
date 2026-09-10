package com.inventory.service.command;

import com.inventory.dao.StockItemDao;
import com.inventory.dao.StockMovementDao;
import com.inventory.model.StockMovement;

import java.sql.Connection;
import java.sql.SQLException;

/** Records a shipment receipt: increases stock and logs an IN movement. */
public class ReceiveStockCommand implements StockCommand {

    private final StockItemDao stockItemDao = new StockItemDao();
    private final StockMovementDao movementDao = new StockMovementDao();

    private final int productId;
    private final int warehouseId;
    private final int poId;
    private final int quantity;
    private final double unitCost;

    public ReceiveStockCommand(int productId, int warehouseId, int poId, int quantity) {
        this(productId, warehouseId, poId, quantity, 0.0);
    }

    public ReceiveStockCommand(int productId, int warehouseId, int poId, int quantity, double unitCost) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.poId = poId;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    @Override
    public void execute(Connection conn) throws SQLException {
        stockItemDao.adjustQuantity(productId, warehouseId, quantity, conn);
        // For a receipt, the purchase cost IS the cost basis for this stock —
        // both unit_price and unit_cost are set to the same value.
        movementDao.insert(new StockMovement(productId, warehouseId, poId, "IN", quantity, unitCost, unitCost,
                "Received against PO #" + poId), conn);
    }
}
