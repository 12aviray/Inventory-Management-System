package com.inventory.service;

import com.inventory.model.StockItem;
import com.inventory.service.strategy.AverageDemandStrategy;
import com.inventory.service.strategy.FixedThresholdStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReorderStrategyTest {

    @Test
    void fixedThreshold_flagsItemBelowThreshold() {
        StockItem item = new StockItem(1, 1, 1, 3);
        item.setReorderThreshold(10);

        FixedThresholdStrategy strategy = new FixedThresholdStrategy();

        assertTrue(strategy.needsReorder(item));
        assertEquals(17, strategy.suggestedOrderQuantity(item)); // target 20, have 3
    }

    @Test
    void fixedThreshold_doesNotFlagItemAboveThreshold() {
        StockItem item = new StockItem(1, 1, 1, 50);
        item.setReorderThreshold(10);

        FixedThresholdStrategy strategy = new FixedThresholdStrategy();

        assertFalse(strategy.needsReorder(item));
        assertEquals(0, strategy.suggestedOrderQuantity(item));
    }

    @Test
    void averageDemand_flagsItemWithInsufficientCoverage() {
        StockItem item = new StockItem(1, 1, 1, 5); // 5 units on hand
        AverageDemandStrategy strategy = new AverageDemandStrategy(2.0, 7); // 2/day, want 7 days cover -> need 14

        assertTrue(strategy.needsReorder(item)); // 5/2 = 2.5 days < 7
        assertEquals(9, strategy.suggestedOrderQuantity(item)); // 14 - 5
    }

    @Test
    void averageDemand_doesNotFlagWithSufficientCoverage() {
        StockItem item = new StockItem(1, 1, 1, 100);
        AverageDemandStrategy strategy = new AverageDemandStrategy(2.0, 7);

        assertFalse(strategy.needsReorder(item));
    }

    @Test
    void strategiesAreInterchangeableViaSameInterface() {
        StockItem item = new StockItem(1, 1, 1, 3);
        item.setReorderThreshold(10);

        RestockingService service = new RestockingService(new FixedThresholdStrategy());
        // Swapping the strategy at runtime should not require any other code change.
        service.setStrategy(new AverageDemandStrategy(1.0, 30));

        assertDoesNotThrow(() -> service.suggestQuantity(item));
    }
}
