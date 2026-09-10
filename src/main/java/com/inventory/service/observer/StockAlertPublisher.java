package com.inventory.service.observer;

import com.inventory.model.StockItem;

import java.util.ArrayList;
import java.util.List;

/** The "subject" in the Observer pattern: holds observers and notifies them. */
public class StockAlertPublisher {

    private final List<StockAlertObserver> observers = new ArrayList<>();

    public void subscribe(StockAlertObserver observer) {
        observers.add(observer);
    }

    public void unsubscribe(StockAlertObserver observer) {
        observers.remove(observer);
    }

    public void notifyLowStock(StockItem stockItem) {
        for (StockAlertObserver observer : observers) {
            observer.onLowStock(stockItem);
        }
    }
}
