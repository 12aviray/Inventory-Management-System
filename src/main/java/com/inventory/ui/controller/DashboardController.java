package com.inventory.ui.controller;

import com.inventory.dao.ProductDao;
import com.inventory.model.StockItem;
import com.inventory.service.RestockingService;
import com.inventory.service.observer.AuditLogObserver;
import com.inventory.service.strategy.FixedThresholdStrategy;
import com.inventory.ui.viewmodel.LowStockRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class DashboardController {

    @FXML private Label lowStockCountLabel;
    @FXML private Label totalProductsLabel;
    @FXML private TableView<LowStockRow> lowStockTable;
    @FXML private TableColumn<LowStockRow, String> productColumn;
    @FXML private TableColumn<LowStockRow, String> warehouseColumn;
    @FXML private TableColumn<LowStockRow, Integer> quantityColumn;
    @FXML private TableColumn<LowStockRow, Integer> thresholdColumn;
    @FXML private TableColumn<LowStockRow, Integer> suggestedColumn;

    // Default to the FixedThresholdStrategy; swappable at runtime (Strategy pattern).
    private final RestockingService restockingService = new RestockingService(new FixedThresholdStrategy());
    private final ProductDao productDao = new ProductDao();
    private final ObservableList<LowStockRow> rows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Register an observer so low-stock events get logged (Observer pattern demo).
        restockingService.getAlertPublisher().subscribe(new AuditLogObserver());

        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        warehouseColumn.setCellValueFactory(new PropertyValueFactory<>("warehouseName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        thresholdColumn.setCellValueFactory(new PropertyValueFactory<>("threshold"));
        suggestedColumn.setCellValueFactory(new PropertyValueFactory<>("suggestedQuantity"));

        lowStockTable.setItems(rows);
        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        var lowStockItems = restockingService.detectLowStock();
        rows.setAll(lowStockItems.stream()
                .map(this::toRow)
                .toList());

        lowStockCountLabel.setText("Items needing reorder: " + lowStockItems.size());
        totalProductsLabel.setText("Total products: " + productDao.findAll().size());
    }

    private LowStockRow toRow(StockItem item) {
        int suggested = restockingService.suggestQuantity(item);
        return new LowStockRow(item.getProductName(), item.getWarehouseName(),
                item.getQuantity(), item.getReorderThreshold(), suggested);
    }
}
