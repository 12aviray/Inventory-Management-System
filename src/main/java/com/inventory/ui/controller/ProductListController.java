package com.inventory.ui.controller;

import com.inventory.model.Product;
import com.inventory.service.ProductService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller for the Product CRUD screen — the "one simple vertical slice"
 * (FXML -> Controller -> Service -> DAO -> SQLite) that all other screens
 * follow the same layered pattern of.
 */
public class ProductListController {

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> skuColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Double> costColumn;
    @FXML private TableColumn<Product, Integer> thresholdColumn;

    @FXML private TextField skuField;
    @FXML private TextField nameField;
    @FXML private TextField categoryField;
    @FXML private TextField costField;
    @FXML private TextField thresholdField;
    @FXML private Label statusLabel;

    private final ProductService productService = new ProductService();
    private final ObservableList<Product> products = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        skuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        costColumn.setCellValueFactory(new PropertyValueFactory<>("unitCost"));
        thresholdColumn.setCellValueFactory(new PropertyValueFactory<>("reorderThreshold"));

        productTable.setItems(products);
        refresh();

        productTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                skuField.setText(selected.getSku());
                nameField.setText(selected.getName());
                categoryField.setText(selected.getCategory());
                costField.setText(String.valueOf(selected.getUnitCost()));
                thresholdField.setText(String.valueOf(selected.getReorderThreshold()));
            }
        });
    }

    private void refresh() {
        products.setAll(productService.findAll());
    }

    @FXML
    private void handleAdd() {
        try {
            Product p = readFormAsProduct();
            productService.create(p);
            refresh();
            clearForm();
            statusLabel.setText("");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a product to update.");
            return;
        }
        try {
            Product p = readFormAsProduct();
            p.setProductId(selected.getProductId());
            productService.update(p);
            refresh();
            statusLabel.setText("");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a product to delete.");
            return;
        }
        productService.delete(selected.getProductId());
        refresh();
        clearForm();
    }

    private Product readFormAsProduct() {
        Product p = new Product();
        p.setSku(skuField.getText());
        p.setName(nameField.getText());
        p.setCategory(categoryField.getText());
        p.setUnitCost(parseDoubleOrThrow(costField.getText(), "Unit cost"));
        p.setReorderThreshold(parseIntOrThrow(thresholdField.getText(), "Reorder threshold"));
        return p;
    }

    private double parseDoubleOrThrow(String text, String fieldName) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a number");
        }
    }

    private int parseIntOrThrow(String text, String fieldName) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number");
        }
    }

    private void clearForm() {
        skuField.clear();
        nameField.clear();
        categoryField.clear();
        costField.clear();
        thresholdField.clear();
    }
}
