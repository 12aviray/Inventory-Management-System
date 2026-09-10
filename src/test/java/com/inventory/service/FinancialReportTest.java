package com.inventory.service;

import com.inventory.dao.ProductDao;
import com.inventory.dao.StockItemDao;
import com.inventory.model.FinancialSummary;
import com.inventory.model.Product;
import com.inventory.model.StockItem;
import com.inventory.service.strategy.FixedThresholdStrategy;
import com.inventory.ui.viewmodel.ProductProfitRow;
import com.inventory.util.DatabaseSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FinancialReportTest {

    private final ProductDao productDao = new ProductDao();
    private final StockItemDao stockItemDao = new StockItemDao();
    private final SalesService salesService = new SalesService();
    private final ReportService reportService = new ReportService(new FixedThresholdStrategy());

    @BeforeEach
    void setUp() {
        DatabaseSeeder.run();
    }

    @Test
    void testFinancialSummaryCalculation() {
        // Product 1: Wireless Mouse (cost = 12.50). Sell 4 units @ $25.00 each.
        // Expected Revenue = 4 * 25.00 = $100.00
        // Expected COGS = 4 * 12.50 = $50.00
        // Expected Profit = $50.00 (50.0% margin)
        salesService.processSale(1, 1, 4, "Financial Test Buyer", 25.00);

        FinancialSummary summary = reportService.getFinancialSummary(null, null, null);
        assertNotNull(summary);
        assertTrue(summary.getTotalRevenue() >= 100.0);
        assertTrue(summary.getTotalCogs() >= 50.0);
        assertTrue(summary.getGrossProfit() >= 50.0);
        assertTrue(summary.getProfitMargin() > 0);
        assertTrue(summary.getInventoryValuation() > 0);
        assertTrue(summary.getTotalUnitsSold() >= 4);

        // Check product-level profit report
        List<ProductProfitRow> profitRows = reportService.getProductProfitReport(null, null, null);
        assertFalse(profitRows.isEmpty());

        ProductProfitRow mouseRow = profitRows.stream()
                .filter(r -> r.getProductId() == 1)
                .findFirst()
                .orElse(null);

        assertNotNull(mouseRow);
        assertTrue(mouseRow.getUnitsSold() >= 4);
        assertEquals(12.50, mouseRow.getUnitCost(), 0.01);
        assertTrue(mouseRow.getRevenue() >= 100.0);
        assertTrue(mouseRow.getProfit() >= 50.0);
    }
}
