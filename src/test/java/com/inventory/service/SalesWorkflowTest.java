package com.inventory.service;

import com.inventory.dao.StockItemDao;
import com.inventory.dao.StockMovementDao;
import com.inventory.model.StockItem;
import com.inventory.model.StockMovement;
import com.inventory.util.DatabaseSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SalesWorkflowTest {

    private final StockItemDao stockItemDao = new StockItemDao();
    private final StockMovementDao movementDao = new StockMovementDao();
    private final SalesService salesService = new SalesService();

    @BeforeEach
    void setUp() {
        DatabaseSeeder.run();
    }

    @Test
    void testProcessDirectSaleSuccess() {
        // Find existing stock in Warehouse 1 for Product 1 (Wireless Mouse)
        StockItem item = stockItemDao.find(1, 1).orElseThrow();
        assertTrue(item.getQuantity() >= 5);
        int initialStock = item.getQuantity();

        // Sell 3 units to a customer
        salesService.processSale(1, 1, 3, "Customer Test LLC", 19.99);

        // Verify stock is decreased by 3
        int updatedStock = stockItemDao.find(1, 1).map(StockItem::getQuantity).orElse(0);
        assertEquals(initialStock - 3, updatedStock);

        // Verify an OUT movement was created
        List<StockMovement> movements = movementDao.search(1, 1, null, null);
        boolean hasOutMovement = movements.stream()
                .anyMatch(m -> "OUT".equals(m.getMovementType()) && m.getQuantity() == 3 && m.getReason().contains("Customer Test LLC"));
        assertTrue(hasOutMovement, "Expected OUT stock movement for customer sale");
    }

    @Test
    void testProcessSaleInsufficientStockThrowsException() {
        StockItem item = stockItemDao.find(1, 1).orElseThrow();
        int available = item.getQuantity();

        // Attempting to sell more than available should throw an exception and not modify stock
        assertThrows(RuntimeException.class, () ->
                salesService.processSale(1, 1, available + 100, "Greedy Customer", 25.00));

        // Ensure stock was NOT modified (transaction rollback)
        int currentStock = stockItemDao.find(1, 1).map(StockItem::getQuantity).orElse(0);
        assertEquals(available, currentStock);
    }

    @Test
    void testProcessBatchCartSale() {
        int initialStock1 = stockItemDao.find(1, 1).map(StockItem::getQuantity).orElse(0);
        int initialStock2 = stockItemDao.find(2, 1).map(StockItem::getQuantity).orElse(0);

        List<SalesService.SaleItem> cart = List.of(
                new SalesService.SaleItem(1, 2, 15.00),
                new SalesService.SaleItem(2, 5, 8.50)
        );

        salesService.processBatchSale(cart, 1, "Multi-item Buyer");

        int updatedStock1 = stockItemDao.find(1, 1).map(StockItem::getQuantity).orElse(0);
        int updatedStock2 = stockItemDao.find(2, 1).map(StockItem::getQuantity).orElse(0);

        assertEquals(initialStock1 - 2, updatedStock1);
        assertEquals(initialStock2 - 5, updatedStock2);
    }
}
