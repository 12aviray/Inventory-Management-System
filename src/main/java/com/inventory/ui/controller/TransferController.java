package com.inventory.ui.controller;

import com.inventory.dao.ProductDao;
import com.inventory.dao.WarehouseDao;
import com.inventory.model.Product;
import com.inventory.model.Warehouse;
import com.inventory.service.TransferService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class TransferController {

    @FXML private ComboBox<Product> productCombo;
    @FXML private ComboBox<Warehouse> fromWarehouseCombo;
    @FXML private ComboBox<Warehouse> toWarehouseCombo;
    @FXML private TextField quantityField;
    @FXML private Label statusLabel;

    private final ProductDao productDao = new ProductDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final TransferService transferService = new TransferService();

    @FXML
    public void initialize() {
        productCombo.setItems(FXCollections.observableArrayList(productDao.findAll()));
        var warehouses = FXCollections.observableArrayList(warehouseDao.findAll());
        fromWarehouseCombo.setItems(warehouses);
        toWarehouseCombo.setItems(warehouses);
    }

    @FXML
    private void handleTransfer() {
        try {
            Product product = productCombo.getValue();
            Warehouse from = fromWarehouseCombo.getValue();
            Warehouse to = toWarehouseCombo.getValue();
            if (product == null || from == null || to == null) {
                statusLabel.setText("Select product, source, and destination warehouse.");
                return;
            }
            int quantity = Integer.parseInt(quantityField.getText());

            transferService.transfer(product.getProductId(), from.getWarehouseId(), to.getWarehouseId(), quantity);

            statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            statusLabel.setText("Transfer completed.");
            quantityField.clear();
        } catch (NumberFormatException e) {
            statusLabel.setTextFill(javafx.scene.paint.Color.CRIMSON);
            statusLabel.setText("Quantity must be a whole number.");
        } catch (Exception e) {
            statusLabel.setTextFill(javafx.scene.paint.Color.CRIMSON);
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
}
