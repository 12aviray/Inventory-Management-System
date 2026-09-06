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

    public ReceiveStockCommand(int productId, int warehouseId, int poId, int quantity) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.poId = poId;
        this.quantity = quantity;
    }

    @Override
    public void execute(Connection conn) throws SQLException {
        stockItemDao.adjustQuantity(productId, warehouseId, quantity, conn);
        movementDao.insert(new StockMovement(productId, warehouseId, poId, "IN", quantity,
                "Received against PO #" + poId), conn);
    }
}
