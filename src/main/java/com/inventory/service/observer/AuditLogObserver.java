package com.inventory.service.observer;

import com.inventory.model.StockItem;

import java.time.LocalDateTime;

/**
 * Example concrete observer: writes low-stock events to a simple log.
 * A second observer (e.g. one that pushes an update to the Dashboard's
 * JavaFX ObservableList) can be added independently.
 */
public class AuditLogObserver implements StockAlertObserver {

    @Override
    public void onLowStock(StockItem stockItem) {
        System.out.printf("[%s] LOW STOCK ALERT: %s at %s has quantity %d (threshold %d)%n",
                LocalDateTime.now(), stockItem.getProductName(), stockItem.getWarehouseName(),
                stockItem.getQuantity(), stockItem.getReorderThreshold());
    }
}
