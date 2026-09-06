package com.inventory.ui.viewmodel;

/** Simple UI-facing row combining a StockItem with its suggested reorder quantity. */
public class LowStockRow {
    private final String productName;
    private final String warehouseName;
    private final int quantity;
    private final int threshold;
    private final int suggestedQuantity;

    public LowStockRow(String productName, String warehouseName, int quantity, int threshold, int suggestedQuantity) {
        this.productName = productName;
        this.warehouseName = warehouseName;
        this.quantity = quantity;
        this.threshold = threshold;
        this.suggestedQuantity = suggestedQuantity;
    }

    public String getProductName() { return productName; }
    public String getWarehouseName() { return warehouseName; }
    public int getQuantity() { return quantity; }
    public int getThreshold() { return threshold; }
    public int getSuggestedQuantity() { return suggestedQuantity; }
}
