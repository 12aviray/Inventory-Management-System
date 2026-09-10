package com.inventory.model;

/**
 * Model representing high-level financial health, profitability, and inventory valuation.
 */
public class FinancialSummary {
    private final double totalRevenue;
    private final double totalCogs;
    private final double grossProfit;
    private final double profitMargin;
    private final double inventoryValuation;
    private final int totalUnitsSold;
    private final double totalProcurementExpense;

    public FinancialSummary(double totalRevenue, double totalCogs, double inventoryValuation,
                            int totalUnitsSold, double totalProcurementExpense) {
        this.totalRevenue = totalRevenue;
        this.totalCogs = totalCogs;
        this.grossProfit = totalRevenue - totalCogs;
        this.profitMargin = totalRevenue > 0 ? (this.grossProfit / totalRevenue) * 100.0 : 0.0;
        this.inventoryValuation = inventoryValuation;
        this.totalUnitsSold = totalUnitsSold;
        this.totalProcurementExpense = totalProcurementExpense;
    }

    public double getTotalRevenue() { return totalRevenue; }
    public double getTotalCogs() { return totalCogs; }
    public double getGrossProfit() { return grossProfit; }
    public double getProfitMargin() { return profitMargin; }
    public double getInventoryValuation() { return inventoryValuation; }
    public int getTotalUnitsSold() { return totalUnitsSold; }
    public double getTotalProcurementExpense() { return totalProcurementExpense; }
}
