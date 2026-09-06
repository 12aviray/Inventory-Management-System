package com.inventory.ui.controller;

import com.inventory.dao.StockItemDao;
import com.inventory.model.StockItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class WarehouseStockController {

    @FXML private TableView<StockItem> stockTable;
    @FXML private TableColumn<StockItem, String> warehouseColumn;
    @FXML private TableColumn<StockItem, String> productColumn;
    @FXML private TableColumn<StockItem, Integer> quantityColumn;
    @FXML private TableColumn<StockItem, Integer> thresholdColumn;
    @FXML private TableColumn<StockItem, String> statusColumn;

    private final StockItemDao stockItemDao = new StockItemDao();
    private final ObservableList<StockItem> items = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        warehouseColumn.setCellValueFactory(new PropertyValueFactory<>("warehouseName"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        thresholdColumn.setCellValueFactory(new PropertyValueFactory<>("reorderThreshold"));
        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().isBelowThreshold() ? "LOW STOCK" : "OK"));

        stockTable.setItems(items);
        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        items.setAll(stockItemDao.findAllWithDetails());
    }
}
