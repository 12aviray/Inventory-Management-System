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

public class TransferWorkflowTest {

    private final StockItemDao stockItemDao = new StockItemDao();
    private final StockMovementDao movementDao = new StockMovementDao();
    private final TransferService transferService = new TransferService();

    @BeforeEach
    void setUp() {
        DatabaseSeeder.run();
    }

    @Test
    void testSuccessfulTransferUpdatesBothWarehousesAndLogsMovements() {
        // Warehouse 1 has Product 2 (quantity 80), Warehouse 2 has Product 2 (0 by default or create)
        int initialSourceQty = stockItemDao.find(2, 1).map(StockItem::getQuantity).orElse(0);
        int initialDestQty = stockItemDao.find(2, 2).map(StockItem::getQuantity).orElse(0);
        assertTrue(initialSourceQty >= 10);

        transferService.transfer(2, 1, 2, 10);

        int newSourceQty = stockItemDao.find(2, 1).map(StockItem::getQuantity).orElse(0);
        int newDestQty = stockItemDao.find(2, 2).map(StockItem::getQuantity).orElse(0);

        assertEquals(initialSourceQty - 10, newSourceQty);
        assertEquals(initialDestQty + 10, newDestQty);

        // Check movements
        List<StockMovement> movements = movementDao.search(2, null, null, null);
        boolean hasOut = movements.stream().anyMatch(m -> "TRANSFER_OUT".equals(m.getMovementType()) && m.getQuantity() == 10);
        boolean hasIn = movements.stream().anyMatch(m -> "TRANSFER_IN".equals(m.getMovementType()) && m.getQuantity() == 10);

        assertTrue(hasOut, "Expected TRANSFER_OUT movement");
        assertTrue(hasIn, "Expected TRANSFER_IN movement");
    }

    @Test
    void testTransferSameWarehouseThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(1, 1, 1, 5));
    }

    @Test
    void testTransferNegativeQuantityThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(1, 1, 2, -5));
    }

    @Test
    void testTransferZeroQuantityThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(1, 1, 2, 0));
    }

    @Test
    void testTransferInsufficientStockThrowsExceptionAndRollsBack() {
        int initialSourceQty = stockItemDao.find(1, 1).map(StockItem::getQuantity).orElse(0);
        int initialDestQty = stockItemDao.find(1, 2).map(StockItem::getQuantity).orElse(0);

        assertThrows(RuntimeException.class, () ->
                transferService.transfer(1, 1, 2, initialSourceQty + 1000));

        int currentSourceQty = stockItemDao.find(1, 1).map(StockItem::getQuantity).orElse(0);
        int currentDestQty = stockItemDao.find(1, 2).map(StockItem::getQuantity).orElse(0);

        assertEquals(initialSourceQty, currentSourceQty, "Source stock must remain unchanged on failure");
        assertEquals(initialDestQty, currentDestQty, "Destination stock must remain unchanged on failure");
    }
}
