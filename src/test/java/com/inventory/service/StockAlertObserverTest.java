package com.inventory.service;

import com.inventory.model.StockItem;
import com.inventory.service.observer.StockAlertObserver;
import com.inventory.service.observer.StockAlertPublisher;
import com.inventory.service.strategy.FixedThresholdStrategy;
import com.inventory.util.DatabaseSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StockAlertObserverTest {

    @BeforeEach
    void setUp() {
        DatabaseSeeder.run();
    }

    @Test
    void testPublisherNotifiesSubscribersOnLowStockDetection() {
        RestockingService service = new RestockingService(new FixedThresholdStrategy());
        List<StockItem> receivedAlerts = new ArrayList<>();

        StockAlertObserver testObserver = receivedAlerts::add;
        service.getAlertPublisher().subscribe(testObserver);

        List<StockItem> detected = service.detectLowStock();

        assertFalse(detected.isEmpty(), "Expected low stock items to be detected");
        assertEquals(detected.size(), receivedAlerts.size(), "Observer should receive notification for every low stock item");
        assertEquals(detected.get(0).getProductId(), receivedAlerts.get(0).getProductId());
    }

    @Test
    void testUnsubscribeStopsNotifications() {
        StockAlertPublisher publisher = new StockAlertPublisher();
        List<StockItem> received = new ArrayList<>();

        StockAlertObserver observer = received::add;
        publisher.subscribe(observer);

        StockItem item1 = new StockItem(1, 1, 5);
        publisher.notifyLowStock(item1);
        assertEquals(1, received.size());

        publisher.unsubscribe(observer);
        StockItem item2 = new StockItem(2, 1, 2);
        publisher.notifyLowStock(item2);

        assertEquals(1, received.size(), "Observer should receive no further notifications after unsubscribe");
    }
}
