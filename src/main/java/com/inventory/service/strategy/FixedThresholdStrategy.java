package com.inventory.service.strategy;

import com.inventory.model.StockItem;

/**
 * Reorder when quantity falls below the product's configured reorder_threshold.
 * Suggests ordering enough to reach 2x the threshold (simple safety-stock rule).
 */
public class FixedThresholdStrategy implements ReorderStrategy {

    @Override
    public boolean needsReorder(StockItem stockItem) {
        return stockItem.isBelowThreshold();
    }

    @Override
    public int suggestedOrderQuantity(StockItem stockItem) {
        int target = stockItem.getReorderThreshold() * 2;
        return Math.max(target - stockItem.getQuantity(), 0);
    }
}
