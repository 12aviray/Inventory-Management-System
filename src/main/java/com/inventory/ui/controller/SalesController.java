package com.inventory.ui.controller;

import com.inventory.dao.ProductDao;
import com.inventory.dao.StockItemDao;
import com.inventory.dao.StockMovementDao;
import com.inventory.dao.WarehouseDao;
import com.inventory.model.Product;
import com.inventory.model.StockItem;
import com.inventory.model.StockMovement;
import com.inventory.model.Warehouse;
import com.inventory.service.SalesService;
import com.inventory.ui.viewmodel.SaleCartRow;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for Sales and Customer Dispatch.
 * Handles item selection, cart management, available stock validation, and batch checkout.
 */
public class SalesController {

    @FXML private TextField customerField;
    @FXML private ComboBox<Warehouse> warehouseCombo;
    @FXML private ComboBox<Product> productCombo;
    @FXML private Label availableStockLabel;
    @FXML private TextField quantityField;
    @FXML private TextField priceField;

    @FXML private TableView<SaleCartRow> cartTable;
    @FXML private TableColumn<SaleCartRow, String> cartProductCol;
    @FXML private TableColumn<SaleCartRow, Integer> cartQtyCol;
    @FXML private TableColumn<SaleCartRow, Double> cartPriceCol;
    @FXML private TableColumn<SaleCartRow, Double> cartSubtotalCol;

    @FXML private Label cartItemCountLabel;
    @FXML private Label cartTotalAmountLabel;
    @FXML private Label statusLabel;

    @FXML private TableView<StockMovement> salesLogTable;
    @FXML private TableColumn<StockMovement, String> logDateCol;
    @FXML private TableColumn<StockMovement, String> logProductCol;
    @FXML private TableColumn<StockMovement, String> logWarehouseCol;
    @FXML private TableColumn<StockMovement, Integer> logQtyCol;
    @FXML private TableColumn<StockMovement, String> logReasonCol;

    private final ProductDao productDao = new ProductDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final StockItemDao stockItemDao = new StockItemDao();
    private final StockMovementDao stockMovementDao = new StockMovementDao();
    private final SalesService salesService = new SalesService();

    private final ObservableList<SaleCartRow> cartRows = FXCollections.observableArrayList();
    private final ObservableList<StockMovement> salesLogRows = FXCollections.observableArrayList();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private int currentAvailableStock = 0;

    @FXML
    public void initialize() {
        warehouseCombo.setItems(FXCollections.observableArrayList(warehouseDao.findAll()));
        productCombo.setItems(FXCollections.observableArrayList(productDao.findAll()));

        // Cart Table Setup
        cartProductCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        cartQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cartPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        cartSubtotalCol.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        cartTable.setItems(cartRows);

        // Sales Log Table Setup
        logDateCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                FORMATTER.format(Instant.ofEpochSecond(cellData.getValue().getCreatedAt()).atZone(ZoneId.systemDefault()))));
        logProductCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        logWarehouseCol.setCellValueFactory(new PropertyValueFactory<>("warehouseName"));
        logQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        logReasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        salesLogTable.setItems(salesLogRows);

        // Selection listeners
        productCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                priceField.setText(String.format("%.2f", newVal.getUnitCost() * 1.25)); // Suggested retail markup
            }
            updateAvailableStock();
        });

        warehouseCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateAvailableStock();
        });

        if (!warehouseCombo.getItems().isEmpty()) {
            warehouseCombo.getSelectionModel().selectFirst();
        }

        handleRefreshSalesLog();
    }

    private void updateAvailableStock() {
        Product p = productCombo.getValue();
        Warehouse w = warehouseCombo.getValue();
        if (p == null || w == null) {
            availableStockLabel.setText("Available: -");
            availableStockLabel.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4;");
            currentAvailableStock = 0;
            return;
        }

        Optional<StockItem> item = stockItemDao.find(p.getProductId(), w.getWarehouseId());
        currentAvailableStock = item.map(StockItem::getQuantity).orElse(0);

        // Account for items already in cart for this product
        int inCart = cartRows.stream()
                .filter(r -> r.getProductId() == p.getProductId())
                .mapToInt(SaleCartRow::getQuantity)
                .sum();

        int netAvailable = currentAvailableStock - inCart;

        if (netAvailable <= 0) {
            availableStockLabel.setText("Available: 0 (Out of stock)");
            availableStockLabel.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: bold;");
        } else {
            availableStockLabel.setText("Available: " + netAvailable + " units");
            availableStockLabel.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleAddToCart() {
        try {
            Product p = productCombo.getValue();
            Warehouse w = warehouseCombo.getValue();
            if (p == null) {
                showStatus("Please select a product to sell.", true);
                return;
            }
            if (w == null) {
                showStatus("Please select a fulfilling warehouse.", true);
                return;
            }

            int qty = Integer.parseInt(quantityField.getText().trim());
            if (qty <= 0) {
                showStatus("Sale quantity must be greater than 0.", true);
                return;
            }

            double price = Double.parseDouble(priceField.getText().trim());
            if (price < 0) {
                showStatus("Price cannot be negative.", true);
                return;
            }

            // Check total quantity against on-hand stock
            int existingInCart = cartRows.stream()
                    .filter(r -> r.getProductId() == p.getProductId())
                    .mapToInt(SaleCartRow::getQuantity)
                    .sum();

            if (existingInCart + qty > currentAvailableStock) {
                showStatus("Cannot add " + qty + " units. Only " + (currentAvailableStock - existingInCart) + " remaining in warehouse.", true);
                return;
            }

            cartRows.add(new SaleCartRow(p.getProductId(), p.getName(), qty, price));
            updateCartTotals();
            updateAvailableStock();

            quantityField.clear();
            showStatus("Added " + qty + "x " + p.getName() + " to cart.", false);
        } catch (NumberFormatException e) {
            showStatus("Quantity and Price must be valid numbers.", true);
        }
    }

    @FXML
    private void handleRemoveFromCart() {
        SaleCartRow selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Select a cart item to remove.", true);
            return;
        }
        cartRows.remove(selected);
        updateCartTotals();
        updateAvailableStock();
        showStatus("Removed item from cart.", false);
    }

    @FXML
    private void handleCheckout() {
        Warehouse w = warehouseCombo.getValue();
        if (w == null) {
            showStatus("Please select a fulfilling warehouse.", true);
            return;
        }
        if (cartRows.isEmpty()) {
            showStatus("Cart is empty. Add products to cart before checkout.", true);
            return;
        }

        try {
            List<SalesService.SaleItem> items = new ArrayList<>();
            for (SaleCartRow row : cartRows) {
                items.add(new SalesService.SaleItem(row.getProductId(), row.getQuantity(), row.getUnitPrice()));
            }

            String customer = customerField.getText() == null ? "" : customerField.getText().trim();
            salesService.processBatchSale(items, w.getWarehouseId(), customer);

            int totalSoldUnits = cartRows.stream().mapToInt(SaleCartRow::getQuantity).sum();
            double totalAmount = cartRows.stream().mapToDouble(SaleCartRow::getSubtotal).sum();

            showStatus(String.format("Sale completed! Dispatched %d units ($%.2f) from %s.",
                    totalSoldUnits, totalAmount, w.getName()), false);

            handleClearCart();
            customerField.clear();
            updateAvailableStock();
            handleRefreshSalesLog();
        } catch (Exception e) {
            showStatus("Sale failed: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleClearCart() {
        cartRows.clear();
        updateCartTotals();
        updateAvailableStock();
    }

    private void updateCartTotals() {
        int count = cartRows.stream().mapToInt(SaleCartRow::getQuantity).sum();
        double total = cartRows.stream().mapToDouble(SaleCartRow::getSubtotal).sum();

        cartItemCountLabel.setText("Total Units: " + count + " (" + cartRows.size() + " line items)");
        cartTotalAmountLabel.setText(String.format("Grand Total: $%.2f", total));
    }

    @FXML
    public void handleRefreshSalesLog() {
        List<StockMovement> list = stockMovementDao.search(null, null, null, null).stream()
                .filter(m -> "OUT".equals(m.getMovementType()))
                .limit(30)
                .toList();
        salesLogRows.setAll(list);
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setStyle(isError ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;" : "-fx-text-fill: #10b981; -fx-font-weight: bold;" );
        statusLabel.setText(message);
    }
}
