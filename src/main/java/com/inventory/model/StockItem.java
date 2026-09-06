package com.inventory.model;

public class StockItem {
    private int stockItemId;
    private int productId;
    private int warehouseId;
    private int quantity;

    // Convenience fields populated by joined queries (not persisted directly)
    private String productName;
    private String warehouseName;
    private int reorderThreshold;

    public StockItem() {
    }

    public StockItem(int stockItemId, int productId, int warehouseId, int quantity) {
        this.stockItemId = stockItemId;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
    }

    public int getStockItemId() { return stockItemId; }
    public void setStockItemId(int stockItemId) { this.stockItemId = stockItemId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getWarehouseId() { return warehouseId; }
    public void setWarehouseId(int warehouseId) { this.warehouseId = warehouseId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }

    public int getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(int reorderThreshold) { this.reorderThreshold = reorderThreshold; }

    public boolean isBelowThreshold() {
        return quantity < reorderThreshold;
    }
}
