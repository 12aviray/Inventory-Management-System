package com.inventory.ui.viewmodel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Editable row for the Receive Shipment screen: wraps a PO line plus a user-entered "receive now" quantity. */
public class ReceiveLineRow {
    private final int poLineId;
    private final int productId;
    private final String productName;
    private final int quantityOrdered;
    private final int quantityAlreadyReceived;
    private final StringProperty receiveNow = new SimpleStringProperty("0");

    public ReceiveLineRow(int poLineId, int productId, String productName,
                           int quantityOrdered, int quantityAlreadyReceived) {
        this.poLineId = poLineId;
        this.productId = productId;
        this.productName = productName;
        this.quantityOrdered = quantityOrdered;
        this.quantityAlreadyReceived = quantityAlreadyReceived;
    }

    public int getPoLineId() { return poLineId; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantityOrdered() { return quantityOrdered; }
    public int getQuantityAlreadyReceived() { return quantityAlreadyReceived; }
    public int getOutstanding() { return quantityOrdered - quantityAlreadyReceived; }

    public String getReceiveNow() { return receiveNow.get(); }
    public void setReceiveNow(String value) { receiveNow.set(value); }
    public StringProperty receiveNowProperty() { return receiveNow; }
}
