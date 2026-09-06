package com.inventory.service.strategy;

import com.inventory.model.StockItem;

/**
 * Strategy pattern.
 *
 * Problem: "is this item low on stock?" can be answered several different ways
 * (a fixed threshold per product; a moving-average-demand-based calculation;
 * later, perhaps a seasonal or supplier-lead-time-aware calculation). Different
 * businesses/categories may want different rules, and the rule used to
 * generate suggested purchase orders needs to be swappable without touching
 * the restocking workflow itself.
 *
 * Why Strategy: the low-stock detector (RestockingService) depends only on
 * this interface, not on a specific algorithm. New algorithms can be added
 * by implementing this interface — no existing code changes (Open/Closed
 * Principle).
 *
 * Alternative considered: a single method with an if/else or switch on an
 * "algorithm type" enum. Rejected because every new algorithm would require
 * editing that method, and unit testing each algorithm in isolation would be
 * harder.
 *
 * Future benefit: adding a "seasonal demand" strategy later requires only a
 * new class; RestockingService and the UI need no modification, only a
 * different strategy instance to be wired in (or, made selectable in the UI
 * as a dropdown).
 */
public interface ReorderStrategy {
    boolean needsReorder(StockItem stockItem);

    /** Suggested quantity to order, given the current stock item. */
    int suggestedOrderQuantity(StockItem stockItem);
}
