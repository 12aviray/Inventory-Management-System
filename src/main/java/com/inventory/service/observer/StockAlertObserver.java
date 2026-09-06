package com.inventory.service.observer;

import com.inventory.model.StockItem;

/**
 * Observer pattern.
 *
 * Problem: when a stock movement causes an item to fall below its reorder
 * threshold, several independent parts of the application may want to react:
 * the Dashboard screen needs to refresh its alert badge, a log/audit
 * component may want to record it, and later an email/SMS notifier might be
 * added. The component that changes stock (RestockingService /
 * StockMovementService) should not need to know about any of these
 * consumers directly.
 *
 * Why Observer: publishers (stock-changing services) simply notify
 * registered observers of a low-stock event; new observers can subscribe
 * without the publisher's code changing at all.
 *
 * Alternative considered: having the stock-update method directly call
 * "dashboard.refresh()" and "log.record()" inline. Rejected because it
 * couples the persistence/business logic layer to specific UI components and
 * makes adding a new notification channel require editing that method again.
 *
 * Future benefit: adding an email notifier for low stock later is a new
 * class implementing this interface, registered with the publisher — no
 * change to RestockingService.
 */
public interface StockAlertObserver {
    void onLowStock(StockItem stockItem);
}
