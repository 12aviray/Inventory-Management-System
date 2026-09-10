package com.inventory.ui.controller;

import com.inventory.dao.ProductDao;
import com.inventory.dao.WarehouseDao;
import com.inventory.model.FinancialSummary;
import com.inventory.model.Product;
import com.inventory.model.StockMovement;
import com.inventory.model.Warehouse;
import com.inventory.service.ReportService;
import com.inventory.service.RestockingService;
import com.inventory.service.strategy.AverageDemandStrategy;
import com.inventory.service.strategy.FixedThresholdStrategy;
import com.inventory.service.strategy.ReorderStrategy;
import com.inventory.ui.viewmodel.LowStockRow;
import com.inventory.ui.viewmodel.ProductProfitRow;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the Reports & Analytics screen.
 * Demonstrates the Strategy pattern by supporting runtime swapping of restocking algorithms,
 * provides filtered audit reporting over stock movements, and generates financial profitability analytics.
 */
public class ReportsController {

    // Tab 1: Low Stock
    @FXML private ComboBox<String> strategyCombo;
    @FXML private Label lowStockSummaryLabel;
    @FXML private TableView<LowStockRow> lowStockTable;
    @FXML private TableColumn<LowStockRow, String> lsProductColumn;
    @FXML private TableColumn<LowStockRow, String> lsWarehouseColumn;
    @FXML private TableColumn<LowStockRow, Integer> lsQuantityColumn;
    @FXML private TableColumn<LowStockRow, Integer> lsThresholdColumn;
    @FXML private TableColumn<LowStockRow, Integer> lsSuggestedColumn;

    // Tab 2: Movement History
    @FXML private ComboBox<Product> filterProductCombo;
    @FXML private ComboBox<Warehouse> filterWarehouseCombo;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Label movementCountLabel;

    @FXML private TableView<StockMovement> movementTable;
    @FXML private TableColumn<StockMovement, String> mvDateColumn;
    @FXML private TableColumn<StockMovement, String> mvProductColumn;
    @FXML private TableColumn<StockMovement, String> mvWarehouseColumn;
    @FXML private TableColumn<StockMovement, String> mvTypeColumn;
    @FXML private TableColumn<StockMovement, Integer> mvQuantityColumn;
    @FXML private TableColumn<StockMovement, String> mvReasonColumn;

    // Tab 3: Financial & Profit/Loss Report
    @FXML private ComboBox<Warehouse> finWarehouseCombo;
    @FXML private DatePicker finFromDatePicker;
    @FXML private DatePicker finToDatePicker;
    @FXML private Label finRevenueLabel;
    @FXML private Label finCogsLabel;
    @FXML private Label finProfitLabel;
    @FXML private Label finMarginLabel;
    @FXML private Label finInventoryValLabel;
    @FXML private Label finUnitsSoldLabel;
    @FXML private Label finProcurementLabel;

    @FXML private TableView<ProductProfitRow> finProfitTable;
    @FXML private TableColumn<ProductProfitRow, String> finProdNameCol;
    @FXML private TableColumn<ProductProfitRow, Integer> finUnitsSoldCol;
    @FXML private TableColumn<ProductProfitRow, String> finUnitCostCol;
    @FXML private TableColumn<ProductProfitRow, String> finAvgPriceCol;
    @FXML private TableColumn<ProductProfitRow, String> finRevenueCol;
    @FXML private TableColumn<ProductProfitRow, String> finCogsCol;
    @FXML private TableColumn<ProductProfitRow, String> finProfitCol;
    @FXML private TableColumn<ProductProfitRow, String> finMarginCol;

    private final ProductDao productDao = new ProductDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();

    private final ObservableList<LowStockRow> lowStockItems = FXCollections.observableArrayList();
    private final ObservableList<StockMovement> movements = FXCollections.observableArrayList();
    private final ObservableList<ProductProfitRow> profitRows = FXCollections.observableArrayList();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ReorderStrategy currentStrategy = new FixedThresholdStrategy();

    @FXML
    public void initialize() {
        // --- Setup Tab 1: Low Stock ---
        lsProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        lsWarehouseColumn.setCellValueFactory(new PropertyValueFactory<>("warehouseName"));
        lsQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        lsThresholdColumn.setCellValueFactory(new PropertyValueFactory<>("threshold"));
        lsSuggestedColumn.setCellValueFactory(new PropertyValueFactory<>("suggestedQuantity"));
        lowStockTable.setItems(lowStockItems);

        strategyCombo.setItems(FXCollections.observableArrayList(
                "Fixed Threshold Strategy (Default)",
                "Average Daily Demand (10 units/day, 5-day cover)",
                "Aggressive Demand (25 units/day, 7-day cover)"
        ));
        strategyCombo.getSelectionModel().selectFirst();
        strategyCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (newVal.startsWith("Average Daily Demand")) {
                    currentStrategy = new AverageDemandStrategy(10.0, 5);
                } else if (newVal.startsWith("Aggressive Demand")) {
                    currentStrategy = new AverageDemandStrategy(25.0, 7);
                } else {
                    currentStrategy = new FixedThresholdStrategy();
                }
                handleLowStockReport();
            }
        });

        // --- Setup Tab 2: Movement History ---
        mvDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                FORMATTER.format(Instant.ofEpochSecond(cellData.getValue().getCreatedAt()).atZone(ZoneId.systemDefault()))));
        mvProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        mvWarehouseColumn.setCellValueFactory(new PropertyValueFactory<>("warehouseName"));
        mvTypeColumn.setCellValueFactory(new PropertyValueFactory<>("movementType"));
        mvQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        mvReasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        movementTable.setItems(movements);

        // Populate filters
        filterProductCombo.setItems(FXCollections.observableArrayList(productDao.findAll()));
        List<Warehouse> warehouses = warehouseDao.findAll();
        filterWarehouseCombo.setItems(FXCollections.observableArrayList(warehouses));

        // --- Setup Tab 3: Financial & Profit/Loss Report ---
        finWarehouseCombo.setItems(FXCollections.observableArrayList(warehouses));
        finProdNameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        finUnitsSoldCol.setCellValueFactory(new PropertyValueFactory<>("unitsSold"));
        finUnitCostCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("$%.2f", cell.getValue().getUnitCost())));
        finAvgPriceCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("$%.2f", cell.getValue().getAvgSellingPrice())));
        finRevenueCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("$%.2f", cell.getValue().getRevenue())));
        finCogsCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("$%.2f", cell.getValue().getCogs())));
        finProfitCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("$%.2f", cell.getValue().getProfit())));
        finMarginCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.1f%%", cell.getValue().getMarginPercentage())));
        finProfitTable.setItems(profitRows);

        // Initial loads
        handleLowStockReport();
        handleMovementReport();
        handleFinancialReport();
    }

    @FXML
    private void handleLowStockReport() {
        RestockingService service = new RestockingService(currentStrategy);
        var rawItems = service.detectLowStock();
        var rows = rawItems.stream()
                .map(item -> new LowStockRow(item.getProductName(), item.getWarehouseName(),
                        item.getQuantity(), item.getReorderThreshold(), service.suggestQuantity(item)))
                .toList();

        lowStockItems.setAll(rows);
        lowStockSummaryLabel.setText(rows.size() + " items needing reorder");
    }

    @FXML
    private void handleMovementReport() {
        Product p = filterProductCombo.getValue();
        Warehouse w = filterWarehouseCombo.getValue();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        Integer productId = p != null ? p.getProductId() : null;
        Integer warehouseId = w != null ? w.getWarehouseId() : null;
        Long fromEpoch = fromDate != null ? fromDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() : null;
        Long toEpoch = toDate != null ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() - 1 : null;

        ReportService reportService = new ReportService(currentStrategy);
        List<StockMovement> list = reportService.movementHistoryReport(productId, warehouseId, fromEpoch, toEpoch);
        movements.setAll(list);
        movementCountLabel.setText("Found " + list.size() + " movements");
    }

    @FXML
    private void handleResetMovementFilters() {
        filterProductCombo.getSelectionModel().clearSelection();
        filterWarehouseCombo.getSelectionModel().clearSelection();
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        handleMovementReport();
    }

    @FXML
    private void handleFinancialReport() {
        Warehouse w = finWarehouseCombo.getValue();
        LocalDate fromDate = finFromDatePicker.getValue();
        LocalDate toDate = finToDatePicker.getValue();

        Integer warehouseId = w != null ? w.getWarehouseId() : null;
        Long fromEpoch = fromDate != null ? fromDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() : null;
        Long toEpoch = toDate != null ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() - 1 : null;

        ReportService reportService = new ReportService(currentStrategy);

        FinancialSummary summary = reportService.getFinancialSummary(warehouseId, fromEpoch, toEpoch);
        finRevenueLabel.setText(String.format("$%.2f", summary.getTotalRevenue()));
        finCogsLabel.setText(String.format("$%.2f", summary.getTotalCogs()));
        finProfitLabel.setText(String.format("$%.2f", summary.getGrossProfit()));
        finMarginLabel.setText(String.format("Margin: %.1f%%", summary.getProfitMargin()));
        finInventoryValLabel.setText(String.format("$%.2f", summary.getInventoryValuation()));
        finUnitsSoldLabel.setText("Units Sold: " + summary.getTotalUnitsSold());
        finProcurementLabel.setText(String.format("$%.2f", summary.getTotalProcurementExpense()));

        List<ProductProfitRow> rows = reportService.getProductProfitReport(warehouseId, fromEpoch, toEpoch);
        profitRows.setAll(rows);
    }

    @FXML
    private void handleResetFinancialFilters() {
        finWarehouseCombo.getSelectionModel().clearSelection();
        finFromDatePicker.setValue(null);
        finToDatePicker.setValue(null);
        handleFinancialReport();
    }
}

