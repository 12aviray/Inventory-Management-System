package com.inventory.model;

public class PurchaseOrderLine {
    private int poLineId;
    private int poId;
    private int productId;
    private String productName; // convenience, from join
    private int quantityOrdered;
    private int quantityReceived;
    private double unitCost;

    public PurchaseOrderLine() {
    }

    public PurchaseOrderLine(int productId, int quantityOrdered, double unitCost) {
        this.productId = productId;
        this.quantityOrdered = quantityOrdered;
        this.unitCost = unitCost;
    }

    public int getPoLineId() { return poLineId; }
    public void setPoLineId(int poLineId) { this.poLineId = poLineId; }

    public int getPoId() { return poId; }
    public void setPoId(int poId) { this.poId = poId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantityOrdered() { return quantityOrdered; }
    public void setQuantityOrdered(int quantityOrdered) { this.quantityOrdered = quantityOrdered; }

    public int getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(int quantityReceived) { this.quantityReceived = quantityReceived; }

    public double getUnitCost() { return unitCost; }
    public void setUnitCost(double unitCost) { this.unitCost = unitCost; }

    public int getOutstandingQuantity() {
        return quantityOrdered - quantityReceived;
    }
}
