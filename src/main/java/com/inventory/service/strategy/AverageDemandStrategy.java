package com.inventory.service.strategy;

import com.inventory.model.StockItem;

/**
 * Reorders when current quantity would not cover a configured number of
 * days of recent average daily demand. This demonstrates the Strategy
 * pattern's value: it needs different inputs (demand history) than
 * FixedThresholdStrategy but is used interchangeably via the same interface.
 */
public class AverageDemandStrategy implements ReorderStrategy {

    private final double averageDailyDemand;
    private final int coverageDaysTarget;

    public AverageDemandStrategy(double averageDailyDemand, int coverageDaysTarget) {
        this.averageDailyDemand = averageDailyDemand;
        this.coverageDaysTarget = coverageDaysTarget;
    }

    @Override
    public boolean needsReorder(StockItem stockItem) {
        double daysOfCoverLeft = averageDailyDemand <= 0
                ? Double.MAX_VALUE
                : stockItem.getQuantity() / averageDailyDemand;
        return daysOfCoverLeft < coverageDaysTarget;
    }

    @Override
    public int suggestedOrderQuantity(StockItem stockItem) {
        int targetQuantity = (int) Math.ceil(averageDailyDemand * coverageDaysTarget);
        return Math.max(targetQuantity - stockItem.getQuantity(), 0);
    }
}
