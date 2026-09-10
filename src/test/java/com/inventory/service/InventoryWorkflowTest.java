package com.inventory.service;

import com.inventory.dao.ProductDao;
import com.inventory.dao.PurchaseOrderDao;
import com.inventory.dao.StockItemDao;
import com.inventory.dao.StockMovementDao;
import com.inventory.model.Product;
import com.inventory.model.PurchaseOrder;
import com.inventory.model.PurchaseOrderBuilder;
import com.inventory.model.StockItem;
import com.inventory.model.StockMovement;
import com.inventory.service.strategy.AverageDemandStrategy;
import com.inventory.service.strategy.FixedThresholdStrategy;
import com.inventory.util.DatabaseSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryWorkflowTest {

    private final ProductDao productDao = new ProductDao();
    private final StockItemDao stockItemDao = new StockItemDao();
    private final StockMovementDao movementDao = new StockMovementDao();
    private final PurchaseOrderDao purchaseOrderDao = new PurchaseOrderDao();

    private final PurchaseOrderService poService = new PurchaseOrderService();
    private final TransferService transferService = new TransferService();

    @BeforeEach
    void setUp() {
        DatabaseSeeder.run();
    }

    @Test
    void testEndToEndPurchaseOrderWorkflow() {
        // 1. Create a new Purchase Order via Builder
        PurchaseOrder po = new PurchaseOrderBuilder()
                .supplier(1)
                .notes("Urgent restock test")
                .addLine(1, 20, 12.50)
                .addLine(2, 30, 4.25)
                .build();

        assertEquals("DRAFT", po.getStatus());
        PurchaseOrder created = poService.create(po);
        assertTrue(created.getPoId() > 0);

        // 2. Advance PO from DRAFT to SENT
        poService.send(created.getPoId());
        PurchaseOrder sent = purchaseOrderDao.findById(created.getPoId()).orElseThrow();
        assertEquals("SENT", sent.getStatus());

        // 3. Receive partial shipment into Warehouse 1
        int initialStock1 = stockItemDao.find(1, 1).map(StockItem::getQuantity).orElse(0);
        int line1Id = sent.getLines().get(0).getPoLineId();
        int line2Id = sent.getLines().get(1).getPoLineId();

        poService.receiveShipment(sent.getPoId(), 1, Map.of(line1Id, 10, line2Id, 15));

        // Verify status is PARTIALLY_RECEIVED
        PurchaseOrder partiallyReceived = purchaseOrderDao.findById(sent.getPoId()).orElseThrow();
        assertEquals("PARTIALLY_RECEIVED", partiallyReceived.getStatus());

        // Verify stock increased
        int updatedStock1 = stockItemDao.find(1, 1).map(StockItem::getQuantity).orElse(0);
        assertEquals(initialStock1 + 10, updatedStock1);

        // 4. Receive remaining shipment
        poService.receiveShipment(sent.getPoId(), 1, Map.of(line1Id, 10, line2Id, 15));

        PurchaseOrder fullyReceived = purchaseOrderDao.findById(sent.getPoId()).orElseThrow();
        assertEquals("RECEIVED", fullyReceived.getStatus());

        // 5. Verify movements were logged for this PO receipt
        List<StockMovement> movements = movementDao.search(1, 1, null, null);
        assertFalse(movements.isEmpty());
        boolean hasPoReceipt = movements.stream()
                .anyMatch(m -> "IN".equals(m.getMovementType()) && m.getPoId() != null && m.getPoId() == sent.getPoId());
        assertTrue(hasPoReceipt, "Expected IN stock movement for PO #" + sent.getPoId());
    }

    @Test
    void testStockTransferWorkflow() {
        // Ensure stock exists in warehouse 1
        StockItem item = stockItemDao.find(1, 1).orElseThrow();
        assertTrue(item.getQuantity() >= 5);

        int initialSourceQty = item.getQuantity();
        int initialDestQty = stockItemDao.find(1, 2).map(StockItem::getQuantity).orElse(0);

        // Transfer 5 units from Warehouse 1 to Warehouse 2
        transferService.transfer(1, 1, 2, 5);

        int updatedSourceQty = stockItemDao.find(1, 1).map(StockItem::getQuantity).orElse(0);
        int updatedDestQty = stockItemDao.find(1, 2).map(StockItem::getQuantity).orElse(0);

        assertEquals(initialSourceQty - 5, updatedSourceQty);
        assertEquals(initialDestQty + 5, updatedDestQty);
    }

    @Test
    void testRestockingStrategies() {
        RestockingService fixedService = new RestockingService(new FixedThresholdStrategy());
        List<StockItem> fixedAlerts = fixedService.detectLowStock();
        assertNotNull(fixedAlerts);

        RestockingService demandService = new RestockingService(new AverageDemandStrategy(15.0, 5));
        List<StockItem> demandAlerts = demandService.detectLowStock();
        assertNotNull(demandAlerts);
    }
}
