package com.inventory.ui.viewmodel;

/**
 * View model representing an item in the active sales checkout cart.
 */
public class SaleCartRow {
    private final int productId;
    private final String productName;
    private final int quantity;
    private final double unitPrice;

    public SaleCartRow(int productId, String productName, int quantity, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getSubtotal() { return quantity * unitPrice; }
}
