package com.inventory.service;

import com.inventory.dao.StockItemDao;
import com.inventory.model.StockItem;
import com.inventory.service.observer.StockAlertPublisher;
import com.inventory.service.strategy.ReorderStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects low-stock items using a pluggable ReorderStrategy (Strategy pattern)
 * and publishes alerts to any subscribed observers (Observer pattern).
 */
public class RestockingService {

    private final StockItemDao stockItemDao = new StockItemDao();
    private final StockAlertPublisher alertPublisher = new StockAlertPublisher();
    private ReorderStrategy strategy;

    public RestockingService(ReorderStrategy strategy) {
        this.strategy = strategy;
    }

    /** Swap the reorder algorithm at runtime — the point of the Strategy pattern. */
    public void setStrategy(ReorderStrategy strategy) {
        this.strategy = strategy;
    }

    public StockAlertPublisher getAlertPublisher() {
        return alertPublisher;
    }

    /** Returns items needing reorder and notifies observers for each. */
    public List<StockItem> detectLowStock() {
        List<StockItem> lowStockItems = new ArrayList<>();
        for (StockItem item : stockItemDao.findAllWithDetails()) {
            if (strategy.needsReorder(item)) {
                lowStockItems.add(item);
                alertPublisher.notifyLowStock(item);
            }
        }
        return lowStockItems;
    }

    public int suggestQuantity(StockItem item) {
        return strategy.suggestedOrderQuantity(item);
    }
}
