package com.inventory.service;

import com.inventory.dao.StockMovementDao;
import com.inventory.model.FinancialSummary;
import com.inventory.model.StockItem;
import com.inventory.model.StockMovement;
import com.inventory.service.strategy.ReorderStrategy;
import com.inventory.ui.viewmodel.ProductProfitRow;
import com.inventory.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportService {

    private final StockMovementDao movementDao = new StockMovementDao();
    private final RestockingService restockingService;

    public ReportService(ReorderStrategy strategy) {
        this.restockingService = new RestockingService(strategy);
    }

    /** Report 1: low-stock items across all warehouses. */
    public List<StockItem> lowStockReport() {
        return restockingService.detectLowStock();
    }

    /** Report 2: stock movement / audit history, optionally filtered. */
    public List<StockMovement> movementHistoryReport(Integer productId, Integer warehouseId,
                                                       Long fromEpoch, Long toEpoch) {
        return movementDao.search(productId, warehouseId, fromEpoch, toEpoch);
    }

    /** Report 3: High-level financial summary (Revenue, COGS, Gross Profit, Valuation).
     *  COGS uses each sale's stored unit_cost snapshot (cost basis at time of sale),
     *  not the product's current unit_cost — see stock_movement.unit_cost in schema.sql. */
    public FinancialSummary getFinancialSummary(Integer warehouseId, Long fromEpoch, Long toEpoch) {
        StringBuilder salesSql = new StringBuilder("""
            SELECT COALESCE(SUM(sm.quantity * sm.unit_price), 0.0) AS total_revenue,
                   COALESCE(SUM(sm.quantity * sm.unit_cost), 0.0) AS total_cogs,
                   COALESCE(SUM(sm.quantity), 0) AS units_sold
            FROM stock_movement sm
            WHERE sm.movement_type = 'OUT'
            """);

        List<Object> salesParams = new ArrayList<>();
        if (warehouseId != null) { salesSql.append(" AND sm.warehouse_id = ?"); salesParams.add(warehouseId); }
        if (fromEpoch != null) { salesSql.append(" AND sm.created_at >= ?"); salesParams.add(fromEpoch); }
        if (toEpoch != null) { salesSql.append(" AND sm.created_at <= ?"); salesParams.add(toEpoch); }

        double totalRevenue = 0;
        double totalCogs = 0;
        int unitsSold = 0;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(salesSql.toString())) {
            for (int i = 0; i < salesParams.size(); i++) ps.setObject(i + 1, salesParams.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalRevenue = rs.getDouble("total_revenue");
                    totalCogs = rs.getDouble("total_cogs");
                    unitsSold = rs.getInt("units_sold");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate sales financials", e);
        }

        // Inventory Valuation
        StringBuilder valSql = new StringBuilder("""
            SELECT COALESCE(SUM(si.quantity * p.unit_cost), 0.0) AS total_val
            FROM stock_item si
            JOIN product p ON p.product_id = si.product_id
            """);
        List<Object> valParams = new ArrayList<>();
        if (warehouseId != null) { valSql.append(" WHERE si.warehouse_id = ?"); valParams.add(warehouseId); }

        double inventoryValuation = 0;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(valSql.toString())) {
            for (int i = 0; i < valParams.size(); i++) ps.setObject(i + 1, valParams.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) inventoryValuation = rs.getDouble("total_val");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate inventory valuation", e);
        }

        // Procurement Expense
        StringBuilder procSql = new StringBuilder("""
            SELECT COALESCE(SUM(sm.quantity * sm.unit_price), 0.0) AS total_proc
            FROM stock_movement sm
            WHERE sm.movement_type = 'IN'
            """);
        List<Object> procParams = new ArrayList<>();
        if (warehouseId != null) { procSql.append(" AND sm.warehouse_id = ?"); procParams.add(warehouseId); }
        if (fromEpoch != null) { procSql.append(" AND sm.created_at >= ?"); procParams.add(fromEpoch); }
        if (toEpoch != null) { procSql.append(" AND sm.created_at <= ?"); procParams.add(toEpoch); }

        double totalProcurement = 0;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(procSql.toString())) {
            for (int i = 0; i < procParams.size(); i++) ps.setObject(i + 1, procParams.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalProcurement = rs.getDouble("total_proc");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate procurement expenses", e);
        }

        return new FinancialSummary(totalRevenue, totalCogs, inventoryValuation, unitsSold, totalProcurement);
    }

    /** Report 4: Per-Product Profitability & Margins.
     *  Note: COGS is summed from each stock_movement's own unit_cost snapshot
     *  (the cost basis at the time each unit was sold), not the product's
     *  current unit_cost — so editing a product's cost later never rewrites
     *  past profit numbers. p.unit_cost is still selected, but only to show
     *  the product's *current* cost for reference alongside the historical figures. */
    public List<ProductProfitRow> getProductProfitReport(Integer warehouseId, Long fromEpoch, Long toEpoch) {
        StringBuilder sql = new StringBuilder("""
            SELECT p.product_id, p.name AS product_name, p.unit_cost,
                   COALESCE(SUM(sm.quantity), 0) AS units_sold,
                   COALESCE(SUM(sm.quantity * sm.unit_price), 0.0) AS total_revenue,
                   COALESCE(SUM(sm.quantity * sm.unit_cost), 0.0) AS total_cogs
            FROM product p
            JOIN stock_movement sm ON sm.product_id = p.product_id AND sm.movement_type = 'OUT'
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();
        if (warehouseId != null) { sql.append(" AND sm.warehouse_id = ?"); params.add(warehouseId); }
        if (fromEpoch != null) { sql.append(" AND sm.created_at >= ?"); params.add(fromEpoch); }
        if (toEpoch != null) { sql.append(" AND sm.created_at <= ?"); params.add(toEpoch); }

        sql.append(" GROUP BY p.product_id, p.name, p.unit_cost ORDER BY total_revenue DESC, p.name ASC");

        List<ProductProfitRow> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int productId = rs.getInt("product_id");
                    String productName = rs.getString("product_name");
                    double unitCost = rs.getDouble("unit_cost");
                    int unitsSold = rs.getInt("units_sold");
                    double revenue = rs.getDouble("total_revenue");
                    double cogs = rs.getDouble("total_cogs");
                    double profit = revenue - cogs;
                    double marginPct = revenue > 0 ? (profit / revenue) * 100.0 : 0.0;
                    double avgPrice = unitsSold > 0 ? (revenue / unitsSold) : 0.0;

                    results.add(new ProductProfitRow(productId, productName, unitsSold, unitCost,
                            avgPrice, revenue, cogs, profit, marginPct));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to generate product profitability report", e);
        }
        return results;
    }
}
