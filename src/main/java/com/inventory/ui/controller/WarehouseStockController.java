package com.inventory.ui.controller;

import com.inventory.dao.StockItemDao;
import com.inventory.dao.WarehouseDao;
import com.inventory.model.StockItem;
import com.inventory.model.Warehouse;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Controller for viewing stock across multiple warehouses.
 * Highlights low-stock items based on each product's configured reorder threshold.
 */
public class WarehouseStockController {

    @FXML private ComboBox<Warehouse> warehouseFilterCombo;
    @FXML private TextField searchField;
    @FXML private Label stockSummaryLabel;

    @FXML private TableView<StockItem> stockTable;
    @FXML private TableColumn<StockItem, String> warehouseColumn;
    @FXML private TableColumn<StockItem, String> productColumn;
    @FXML private TableColumn<StockItem, Integer> quantityColumn;
    @FXML private TableColumn<StockItem, Integer> thresholdColumn;
    @FXML private TableColumn<StockItem, String> statusColumn;

    private final StockItemDao stockItemDao = new StockItemDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();

    private final ObservableList<StockItem> masterStockList = FXCollections.observableArrayList();
    private FilteredList<StockItem> filteredStockList;

    @FXML
    public void initialize() {
        warehouseFilterCombo.setItems(FXCollections.observableArrayList(warehouseDao.findAll()));

        warehouseColumn.setCellValueFactory(new PropertyValueFactory<>("warehouseName"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        thresholdColumn.setCellValueFactory(new PropertyValueFactory<>("reorderThreshold"));

        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isBelowThreshold() ? "LOW STOCK" : "OK"));

        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("LOW STOCK".equals(item)) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    }
                }
            }
        });

        filteredStockList = new FilteredList<>(masterStockList, item -> true);
        stockTable.setItems(filteredStockList);

        warehouseFilterCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        List<StockItem> list = stockItemDao.findAllWithDetails();
        masterStockList.setAll(list);
        updateSummary();
    }

    private void applyFilters() {
        Warehouse selectedWarehouse = warehouseFilterCombo.getValue();
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        filteredStockList.setPredicate(item -> {
            boolean matchesWarehouse = (selectedWarehouse == null)
                    || (item.getWarehouseId() == selectedWarehouse.getWarehouseId());

            boolean matchesSearch = searchText.isEmpty()
                    || (item.getProductName() != null && item.getProductName().toLowerCase().contains(searchText));

            return matchesWarehouse && matchesSearch;
        });

        updateSummary();
    }

    @FXML
    private void handleClearFilters() {
        warehouseFilterCombo.getSelectionModel().clearSelection();
        searchField.clear();
        applyFilters();
    }

    private void updateSummary() {
        int totalUnits = filteredStockList.stream().mapToInt(StockItem::getQuantity).sum();
        long lowStockCount = filteredStockList.stream().filter(StockItem::isBelowThreshold).count();

        stockSummaryLabel.setText(String.format("Showing %d entries | Total Units: %d | Low Stock: %d",
                filteredStockList.size(), totalUnits, lowStockCount));
    }
}
