package com.inventory.model;

public class StockMovement {
    private int movementId;
    private int productId;
    private String productName;
    private int warehouseId;
    private String warehouseName;
    private Integer poId; // nullable
    private String movementType; // IN, OUT, TRANSFER_OUT, TRANSFER_IN, ADJUSTMENT
    private int quantity;
    private double unitPrice;
    private String reason;
    private long createdAt;

    public StockMovement() {
    }

    public StockMovement(int productId, int warehouseId, Integer poId, String movementType, int quantity, String reason) {
        this(productId, warehouseId, poId, movementType, quantity, 0.0, reason);
    }

    public StockMovement(int productId, int warehouseId, Integer poId, String movementType, int quantity, double unitPrice, String reason) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.poId = poId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.reason = reason;
    }

    public int getMovementId() { return movementId; }
    public void setMovementId(int movementId) { this.movementId = movementId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getWarehouseId() { return warehouseId; }
    public void setWarehouseId(int warehouseId) { this.warehouseId = warehouseId; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }

    public Integer getPoId() { return poId; }
    public void setPoId(Integer poId) { this.poId = poId; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
