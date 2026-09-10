package com.inventory.ui.viewmodel;

/**
 * View model representing a per-product profit & loss breakdown row for reporting.
 */
public class ProductProfitRow {
    private final int productId;
    private final String productName;
    private final int unitsSold;
    private final double unitCost;
    private final double avgSellingPrice;
    private final double revenue;
    private final double cogs;
    private final double profit;
    private final double marginPercentage;

    public ProductProfitRow(int productId, String productName, int unitsSold, double unitCost,
                            double avgSellingPrice, double revenue, double cogs, double profit, double marginPercentage) {
        this.productId = productId;
        this.productName = productName;
        this.unitsSold = unitsSold;
        this.unitCost = unitCost;
        this.avgSellingPrice = avgSellingPrice;
        this.revenue = revenue;
        this.cogs = cogs;
        this.profit = profit;
        this.marginPercentage = marginPercentage;
    }

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getUnitsSold() { return unitsSold; }
    public double getUnitCost() { return unitCost; }
    public double getAvgSellingPrice() { return avgSellingPrice; }
    public double getRevenue() { return revenue; }
    public double getCogs() { return cogs; }
    public double getProfit() { return profit; }
    public double getMarginPercentage() { return marginPercentage; }
}
