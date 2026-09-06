package com.inventory.service;

import com.inventory.dao.StockMovementDao;
import com.inventory.model.StockItem;
import com.inventory.model.StockMovement;
import com.inventory.service.strategy.ReorderStrategy;

import java.util.List;

public class ReportService {

    private final StockMovementDao movementDao = new StockMovementDao();
    private final RestockingService restockingService;

    public ReportService(ReorderStrategy strategy) {
        this.restockingService = new RestockingService(strategy);
    }

    /** Report 1: low-stock items across all warehouses. */
    public List<StockItem> lowStockReport() {
        return restockingService.detectLowStock();
    }

    /** Report 2: stock movement / audit history, optionally filtered. */
    public List<StockMovement> movementHistoryReport(Integer productId, Integer warehouseId,
                                                       Long fromEpoch, Long toEpoch) {
        return movementDao.search(productId, warehouseId, fromEpoch, toEpoch);
    }
}
