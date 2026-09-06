package com.inventory.ui.controller;

import com.inventory.model.StockItem;
import com.inventory.model.StockMovement;
import com.inventory.service.ReportService;
import com.inventory.service.strategy.FixedThresholdStrategy;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ReportsController {

    @FXML private TableView<StockItem> lowStockTable;
    @FXML private TableColumn<StockItem, String> lsProductColumn;
    @FXML private TableColumn<StockItem, String> lsWarehouseColumn;
    @FXML private TableColumn<StockItem, Integer> lsQuantityColumn;
    @FXML private TableColumn<StockItem, Integer> lsThresholdColumn;

    @FXML private TableView<StockMovement> movementTable;
    @FXML private TableColumn<StockMovement, String> mvDateColumn;
    @FXML private TableColumn<StockMovement, String> mvProductColumn;
    @FXML private TableColumn<StockMovement, String> mvWarehouseColumn;
    @FXML private TableColumn<StockMovement, String> mvTypeColumn;
    @FXML private TableColumn<StockMovement, Integer> mvQuantityColumn;

    // Reporting uses the same Strategy-driven RestockingService under the hood.
    private final ReportService reportService = new ReportService(new FixedThresholdStrategy());
    private final ObservableList<StockItem> lowStockItems = FXCollections.observableArrayList();
    private final ObservableList<StockMovement> movements = FXCollections.observableArrayList();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        lsProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        lsWarehouseColumn.setCellValueFactory(new PropertyValueFactory<>("warehouseName"));
        lsQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        lsThresholdColumn.setCellValueFactory(new PropertyValueFactory<>("reorderThreshold"));
        lowStockTable.setItems(lowStockItems);

        mvProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        mvWarehouseColumn.setCellValueFactory(new PropertyValueFactory<>("warehouseName"));
        mvTypeColumn.setCellValueFactory(new PropertyValueFactory<>("movementType"));
        mvQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        mvDateColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                FORMATTER.format(Instant.ofEpochSecond(cellData.getValue().getCreatedAt()).atZone(ZoneId.systemDefault()))));
        movementTable.setItems(movements);
    }

    @FXML
    private void handleLowStockReport() {
        lowStockItems.setAll(reportService.lowStockReport());
    }

    @FXML
    private void handleMovementReport() {
        // No filters applied here (nulls = "all"); a filter UI can be added by
        // wiring TextFields/DatePickers to the same ReportService.movementHistoryReport(...) call.
        movements.setAll(reportService.movementHistoryReport(null, null, null, null));
    }
}
