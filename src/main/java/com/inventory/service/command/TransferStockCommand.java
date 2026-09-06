package com.inventory.service.command;

import com.inventory.dao.StockItemDao;
import com.inventory.dao.StockMovementDao;
import com.inventory.model.StockItem;
import com.inventory.model.StockMovement;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/** Moves stock from one warehouse to another: two movements, one transaction. */
public class TransferStockCommand implements StockCommand {

    private final StockItemDao stockItemDao = new StockItemDao();
    private final StockMovementDao movementDao = new StockMovementDao();

    private final int productId;
    private final int fromWarehouseId;
    private final int toWarehouseId;
    private final int quantity;

    public TransferStockCommand(int productId, int fromWarehouseId, int toWarehouseId, int quantity) {
        this.productId = productId;
        this.fromWarehouseId = fromWarehouseId;
        this.toWarehouseId = toWarehouseId;
        this.quantity = quantity;
    }

    @Override
    public void execute(Connection conn) throws SQLException {
        Optional<StockItem> source = stockItemDao.find(productId, fromWarehouseId);
        int available = source.map(StockItem::getQuantity).orElse(0);
        if (available < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock at source warehouse: have " + available + ", need " + quantity);
        }

        stockItemDao.adjustQuantity(productId, fromWarehouseId, -quantity, conn);
        movementDao.insert(new StockMovement(productId, fromWarehouseId, null, "TRANSFER_OUT", quantity,
                "Transfer to warehouse " + toWarehouseId), conn);

        stockItemDao.adjustQuantity(productId, toWarehouseId, quantity, conn);
        movementDao.insert(new StockMovement(productId, toWarehouseId, null, "TRANSFER_IN", quantity,
                "Transfer from warehouse " + fromWarehouseId), conn);
    }
}
