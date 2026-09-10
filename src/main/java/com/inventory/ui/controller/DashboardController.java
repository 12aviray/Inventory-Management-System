package com.inventory.ui.controller;

import com.inventory.dao.ProductDao;
import com.inventory.dao.PurchaseOrderDao;
import com.inventory.dao.StockItemDao;
import com.inventory.model.FinancialSummary;
import com.inventory.model.PurchaseOrder;
import com.inventory.model.StockItem;
import com.inventory.service.ReportService;
import com.inventory.service.RestockingService;
import com.inventory.service.observer.AuditLogObserver;
import com.inventory.service.strategy.AverageDemandStrategy;
import com.inventory.service.strategy.FixedThresholdStrategy;
import com.inventory.service.strategy.ReorderStrategy;
import com.inventory.ui.viewmodel.LowStockRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Controller for the Executive Inventory Dashboard.
 * Integrates Strategy pattern for live reorder calculations, Observer pattern for
 * alerts, and displays key operational metrics across the system.
 */
public class DashboardController {

    @FXML private ComboBox<String> dashboardStrategyCombo;
    @FXML private Label totalRevenueLabel;
    @FXML private Label grossProfitLabel;
    @FXML private Label profitMarginLabel;
    @FXML private Label inventoryValueLabel;
    @FXML private Label totalUnitsSoldLabel;

    @FXML private Label totalProductsLabel;
    @FXML private Label lowStockCountLabel;
    @FXML private Label pendingOrdersLabel;
    @FXML private Label totalUnitsLabel;
    @FXML private Label activeStrategyBadge;

    @FXML private TableView<LowStockRow> lowStockTable;
    @FXML private TableColumn<LowStockRow, String> productColumn;
    @FXML private TableColumn<LowStockRow, String> warehouseColumn;
    @FXML private TableColumn<LowStockRow, Integer> quantityColumn;
    @FXML private TableColumn<LowStockRow, Integer> thresholdColumn;
    @FXML private TableColumn<LowStockRow, Integer> suggestedColumn;

    private final ProductDao productDao = new ProductDao();
    private final StockItemDao stockItemDao = new StockItemDao();
    private final PurchaseOrderDao purchaseOrderDao = new PurchaseOrderDao();

    private ReorderStrategy activeStrategy = new FixedThresholdStrategy();
    private final ObservableList<LowStockRow> rows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        warehouseColumn.setCellValueFactory(new PropertyValueFactory<>("warehouseName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        thresholdColumn.setCellValueFactory(new PropertyValueFactory<>("threshold"));
        suggestedColumn.setCellValueFactory(new PropertyValueFactory<>("suggestedQuantity"));
        lowStockTable.setItems(rows);

        dashboardStrategyCombo.setItems(FXCollections.observableArrayList(
                "Fixed Threshold Strategy",
                "Average Daily Demand (10 units/day, 5-day cover)",
                "Aggressive Demand (25 units/day, 7-day cover)"
        ));
        dashboardStrategyCombo.getSelectionModel().selectFirst();
        dashboardStrategyCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (newVal.startsWith("Average Daily Demand")) {
                    activeStrategy = new AverageDemandStrategy(10.0, 5);
                } else if (newVal.startsWith("Aggressive Demand")) {
                    activeStrategy = new AverageDemandStrategy(25.0, 7);
                } else {
                    activeStrategy = new FixedThresholdStrategy();
                }
                handleRefresh();
            }
        });

        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        ReportService reportService = new ReportService(activeStrategy);
        RestockingService restockingService = new RestockingService(activeStrategy);
        // Subscribe the audit log observer for this evaluation run
        restockingService.getAlertPublisher().subscribe(new AuditLogObserver());

        var lowStockItems = restockingService.detectLowStock();
        rows.setAll(lowStockItems.stream()
                .map(item -> toRow(item, restockingService))
                .toList());

        // Update Financial KPIs
        FinancialSummary financials = reportService.getFinancialSummary(null, null, null);
        totalRevenueLabel.setText(String.format("$%.2f", financials.getTotalRevenue()));
        grossProfitLabel.setText(String.format("$%.2f", financials.getGrossProfit()));
        profitMarginLabel.setText(String.format("Margin: %.1f%%", financials.getProfitMargin()));
        inventoryValueLabel.setText(String.format("$%.2f", financials.getInventoryValuation()));
        totalUnitsSoldLabel.setText(financials.getTotalUnitsSold() + " units");

        // Update Operational KPI counters
        int totalProducts = productDao.findAll().size();
        totalProductsLabel.setText(String.valueOf(totalProducts));

        lowStockCountLabel.setText(String.valueOf(lowStockItems.size()));

        List<PurchaseOrder> allPOs = purchaseOrderDao.findAll();
        long activeOrders = allPOs.stream()
                .filter(po -> "DRAFT".equals(po.getStatus()) || "SENT".equals(po.getStatus()) || "PARTIALLY_RECEIVED".equals(po.getStatus()))
                .count();
        pendingOrdersLabel.setText(String.valueOf(activeOrders));

        int totalUnits = stockItemDao.findAllWithDetails().stream()
                .mapToInt(StockItem::getQuantity)
                .sum();
        totalUnitsLabel.setText(String.valueOf(totalUnits));

        activeStrategyBadge.setText("Rule: " + activeStrategy.getClass().getSimpleName());
    }

    private LowStockRow toRow(StockItem item, RestockingService service) {
        int suggested = service.suggestQuantity(item);
        int target = activeStrategy instanceof FixedThresholdStrategy
                ? item.getReorderThreshold()
                : (int) Math.ceil(service.suggestQuantity(item) + item.getQuantity());
        return new LowStockRow(item.getProductName(), item.getWarehouseName(),
                item.getQuantity(), target, suggested);
    }
}
