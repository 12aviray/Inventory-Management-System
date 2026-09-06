package com.inventory.model;

public class Product {
    private int productId;
    private String sku;
    private String name;
    private String category;
    private double unitCost;
    private int reorderThreshold;

    public Product() {
    }

    public Product(int productId, String sku, String name, String category,
                    double unitCost, int reorderThreshold) {
        this.productId = productId;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.unitCost = unitCost;
        this.reorderThreshold = reorderThreshold;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getUnitCost() { return unitCost; }
    public void setUnitCost(double unitCost) { this.unitCost = unitCost; }

    public int getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(int reorderThreshold) { this.reorderThreshold = reorderThreshold; }

    @Override
    public String toString() {
        return name + " (" + sku + ")";
    }
}
