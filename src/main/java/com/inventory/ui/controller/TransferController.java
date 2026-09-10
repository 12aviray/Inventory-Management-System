package com.inventory.ui.controller;

import com.inventory.dao.ProductDao;
import com.inventory.dao.StockItemDao;
import com.inventory.dao.StockMovementDao;
import com.inventory.dao.WarehouseDao;
import com.inventory.model.Product;
import com.inventory.model.StockItem;
import com.inventory.model.StockMovement;
import com.inventory.model.Warehouse;
import com.inventory.service.TransferService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for inter-warehouse stock transfers.
 * Executes TransferStockCommand within a transaction to maintain movement audit history.
 */
public class TransferController {

    @FXML private ComboBox<Product> productCombo;
    @FXML private ComboBox<Warehouse> fromWarehouseCombo;
    @FXML private ComboBox<Warehouse> toWarehouseCombo;
    @FXML private Label availableStockLabel;
    @FXML private TextField quantityField;
    @FXML private Label statusLabel;

    @FXML private TableView<StockMovement> recentTransfersTable;
    @FXML private TableColumn<StockMovement, String> tfDateColumn;
    @FXML private TableColumn<StockMovement, String> tfProductColumn;
    @FXML private TableColumn<StockMovement, String> tfWarehouseColumn;
    @FXML private TableColumn<StockMovement, String> tfTypeColumn;
    @FXML private TableColumn<StockMovement, Integer> tfQtyColumn;
    @FXML private TableColumn<StockMovement, String> tfReasonColumn;

    private final ProductDao productDao = new ProductDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final StockItemDao stockItemDao = new StockItemDao();
    private final StockMovementDao stockMovementDao = new StockMovementDao();
    private final TransferService transferService = new TransferService();

    private final ObservableList<StockMovement> transferMovements = FXCollections.observableArrayList();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int currentAvailableStock = 0;

    @FXML
    public void initialize() {
        productCombo.setItems(FXCollections.observableArrayList(productDao.findAll()));
        List<Warehouse> warehouses = warehouseDao.findAll();
        fromWarehouseCombo.setItems(FXCollections.observableArrayList(warehouses));
        toWarehouseCombo.setItems(FXCollections.observableArrayList(warehouses));

        productCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateStockAvailability());
        fromWarehouseCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateStockAvailability());

        // Setup Recent Transfers Table
        tfDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                FORMATTER.format(Instant.ofEpochSecond(cellData.getValue().getCreatedAt()).atZone(ZoneId.systemDefault()))));
        tfProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        tfWarehouseColumn.setCellValueFactory(new PropertyValueFactory<>("warehouseName"));
        tfTypeColumn.setCellValueFactory(new PropertyValueFactory<>("movementType"));
        tfQtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        tfReasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));

        recentTransfersTable.setItems(transferMovements);
        refreshRecentTransfers();
    }

    private void updateStockAvailability() {
        Product p = productCombo.getValue();
        Warehouse w = fromWarehouseCombo.getValue();
        if (p == null || w == null) {
            availableStockLabel.setText("Available Stock: -");
            availableStockLabel.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4;");
            currentAvailableStock = 0;
            return;
        }

        Optional<StockItem> item = stockItemDao.find(p.getProductId(), w.getWarehouseId());
        currentAvailableStock = item.map(StockItem::getQuantity).orElse(0);

        if (currentAvailableStock <= 0) {
            availableStockLabel.setText("Available Stock: 0 units (Out of Stock)");
            availableStockLabel.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: bold;");
        } else {
            availableStockLabel.setText("Available Stock: " + currentAvailableStock + " units");
            availableStockLabel.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleTransferAll() {
        if (currentAvailableStock > 0) {
            quantityField.setText(String.valueOf(currentAvailableStock));
        } else {
            showStatus("No stock available at the selected source warehouse.", true);
        }
    }

    @FXML
    private void handleTransfer() {
        try {
            Product product = productCombo.getValue();
            Warehouse from = fromWarehouseCombo.getValue();
            Warehouse to = toWarehouseCombo.getValue();

            if (product == null) {
                showStatus("Please select a product to transfer.", true);
                return;
            }
            if (from == null || to == null) {
                showStatus("Please select both source and destination warehouses.", true);
                return;
            }
            if (from.getWarehouseId() == to.getWarehouseId()) {
                showStatus("Source and destination warehouses cannot be the same.", true);
                return;
            }

            String qtyText = quantityField.getText() == null ? "" : quantityField.getText().trim();
            if (qtyText.isEmpty()) {
                showStatus("Please enter a transfer quantity.", true);
                return;
            }

            int quantity = Integer.parseInt(qtyText);
            if (quantity <= 0) {
                showStatus("Transfer quantity must be greater than 0.", true);
                return;
            }

            if (quantity > currentAvailableStock) {
                showStatus("Insufficient stock: available " + currentAvailableStock + ", requested " + quantity, true);
                return;
            }

            transferService.transfer(product.getProductId(), from.getWarehouseId(), to.getWarehouseId(), quantity);

            showStatus("Successfully transferred " + quantity + " units of " + product.getName() + " from " + from.getName() + " to " + to.getName() + "!", false);
            quantityField.clear();
            updateStockAvailability();
            refreshRecentTransfers();
        } catch (NumberFormatException e) {
            showStatus("Quantity must be a valid whole number.", true);
        } catch (Exception e) {
            showStatus("Transfer failed: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleClear() {
        productCombo.getSelectionModel().clearSelection();
        fromWarehouseCombo.getSelectionModel().clearSelection();
        toWarehouseCombo.getSelectionModel().clearSelection();
        quantityField.clear();
        availableStockLabel.setText("Available Stock: -");
        statusLabel.setText("");
        currentAvailableStock = 0;
    }

    private void refreshRecentTransfers() {
        List<StockMovement> list = stockMovementDao.search(null, null, null, null).stream()
                .filter(m -> "TRANSFER_OUT".equals(m.getMovementType()) || "TRANSFER_IN".equals(m.getMovementType()))
                .limit(25)
                .toList();
        transferMovements.setAll(list);
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setStyle(isError ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;" : "-fx-text-fill: #10b981; -fx-font-weight: bold;");
        statusLabel.setText(message);
    }
}
