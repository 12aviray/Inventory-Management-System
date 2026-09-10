package com.inventory.ui.controller;

import com.inventory.model.Product;
import com.inventory.service.ProductService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Controller for the Product Management CRUD screen.
 * Provides product creation, editing, deletion, and real-time search filtering.
 */
public class ProductListController {

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> skuColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Double> costColumn;
    @FXML private TableColumn<Product, Integer> thresholdColumn;

    @FXML private TextField searchField;
    @FXML private TextField skuField;
    @FXML private TextField nameField;
    @FXML private TextField categoryField;
    @FXML private TextField costField;
    @FXML private TextField thresholdField;
    @FXML private Label statusLabel;

    private final ProductService productService = new ProductService();
    private final ObservableList<Product> masterProducts = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;

    @FXML
    public void initialize() {
        skuColumn.setCellValueFactory(new PropertyValueFactory<>("sku"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        costColumn.setCellValueFactory(new PropertyValueFactory<>("unitCost"));
        thresholdColumn.setCellValueFactory(new PropertyValueFactory<>("reorderThreshold"));

        filteredProducts = new FilteredList<>(masterProducts, p -> true);
        productTable.setItems(filteredProducts);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredProducts.setPredicate(product -> {
                    if (newVal == null || newVal.isBlank()) return true;
                    String filter = newVal.toLowerCase().trim();
                    return (product.getName() != null && product.getName().toLowerCase().contains(filter))
                            || (product.getSku() != null && product.getSku().toLowerCase().contains(filter))
                            || (product.getCategory() != null && product.getCategory().toLowerCase().contains(filter));
                });
            });
        }

        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                skuField.setText(selected.getSku());
                nameField.setText(selected.getName());
                categoryField.setText(selected.getCategory());
                costField.setText(String.format("%.2f", selected.getUnitCost()));
                thresholdField.setText(String.valueOf(selected.getReorderThreshold()));
                statusLabel.setText("");
            }
        });

        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        int prevId = selected != null ? selected.getProductId() : -1;

        List<Product> all = productService.findAll();
        masterProducts.setAll(all);

        if (prevId != -1) {
            for (Product p : all) {
                if (p.getProductId() == prevId) {
                    productTable.getSelectionModel().select(p);
                    break;
                }
            }
        }
    }

    @FXML
    private void handleClearSearch() {
        if (searchField != null) {
            searchField.clear();
        }
    }

    @FXML
    private void handleAdd() {
        try {
            Product p = readFormAsProduct();
            productService.create(p);
            handleRefresh();
            handleClearForm();
            showStatus("Product '" + p.getName() + "' created successfully!", false);
        } catch (IllegalArgumentException e) {
            showStatus(e.getMessage(), true);
        } catch (Exception e) {
            String msg = e.getMessage() != null && e.getMessage().contains("UNIQUE")
                    ? "A product with SKU '" + skuField.getText().trim() + "' already exists."
                    : "Error creating product: " + e.getMessage();
            showStatus(msg, true);
        }
    }

    @FXML
    private void handleUpdate() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Select a product from the table to update.", true);
            return;
        }
        try {
            Product p = readFormAsProduct();
            p.setProductId(selected.getProductId());
            productService.update(p);
            handleRefresh();
            showStatus("Product '" + p.getName() + "' updated successfully!", false);
        } catch (IllegalArgumentException e) {
            showStatus(e.getMessage(), true);
        } catch (Exception e) {
            String msg = e.getMessage() != null && e.getMessage().contains("UNIQUE")
                    ? "A product with SKU '" + skuField.getText().trim() + "' already exists."
                    : "Error updating product: " + e.getMessage();
            showStatus(msg, true);
        }
    }

    @FXML
    private void handleDelete() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Select a product from the table to delete.", true);
            return;
        }
        try {
            productService.delete(selected.getProductId());
            handleRefresh();
            handleClearForm();
            showStatus("Product '" + selected.getName() + "' deleted successfully.", false);
        } catch (Exception e) {
            showStatus("Cannot delete product: It is referenced in stock items, purchase orders, or movements.", true);
        }
    }

    @FXML
    private void handleClearForm() {
        productTable.getSelectionModel().clearSelection();
        skuField.clear();
        nameField.clear();
        categoryField.clear();
        costField.clear();
        thresholdField.clear();
        statusLabel.setText("");
    }

    private Product readFormAsProduct() {
        String sku = skuField.getText() == null ? "" : skuField.getText().trim();
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String category = categoryField.getText() == null ? "" : categoryField.getText().trim();

        if (sku.isEmpty()) throw new IllegalArgumentException("SKU cannot be empty.");
        if (name.isEmpty()) throw new IllegalArgumentException("Product Name cannot be empty.");
        if (category.isEmpty()) throw new IllegalArgumentException("Category cannot be empty.");

        double cost = parseDoubleOrThrow(costField.getText(), "Unit cost");
        int threshold = parseIntOrThrow(thresholdField.getText(), "Reorder threshold");

        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setCategory(category);
        p.setUnitCost(cost);
        p.setReorderThreshold(threshold);
        return p;
    }

    private double parseDoubleOrThrow(String text, String fieldName) {
        try {
            double val = Double.parseDouble(text.trim());
            if (val < 0) throw new IllegalArgumentException(fieldName + " cannot be negative.");
            return val;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    private int parseIntOrThrow(String text, String fieldName) {
        try {
            int val = Integer.parseInt(text.trim());
            if (val < 0) throw new IllegalArgumentException(fieldName + " cannot be negative.");
            return val;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid whole number.");
        }
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setStyle(isError ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;" : "-fx-text-fill: #10b981; -fx-font-weight: bold;");
        statusLabel.setText(message);
    }
}
