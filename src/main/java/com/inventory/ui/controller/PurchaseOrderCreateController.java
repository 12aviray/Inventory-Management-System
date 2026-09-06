package com.inventory.ui.controller;

import com.inventory.dao.ProductDao;
import com.inventory.dao.SupplierDao;
import com.inventory.model.Product;
import com.inventory.model.PurchaseOrderBuilder;
import com.inventory.model.PurchaseOrderLine;
import com.inventory.model.Supplier;
import com.inventory.service.PurchaseOrderService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates the Builder pattern in use: lines are added incrementally from
 * the UI as the user fills the form, then PurchaseOrderBuilder.build() validates
 * and produces the final PurchaseOrder on submit.
 */
public class PurchaseOrderCreateController {

    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private TextField notesField;
    @FXML private ComboBox<Product> productCombo;
    @FXML private TextField lineQuantityField;
    @FXML private TextField lineCostField;

    @FXML private TableView<PurchaseOrderLine> linesTable;
    @FXML private TableColumn<PurchaseOrderLine, String> lineProductColumn;
    @FXML private TableColumn<PurchaseOrderLine, Integer> lineQuantityColumn;
    @FXML private TableColumn<PurchaseOrderLine, Double> lineCostColumn;

    @FXML private Label statusLabel;

    private final SupplierDao supplierDao = new SupplierDao();
    private final ProductDao productDao = new ProductDao();
    private final PurchaseOrderService purchaseOrderService = new PurchaseOrderService();

    private final ObservableList<PurchaseOrderLine> pendingLines = FXCollections.observableArrayList();
    private final Map<Integer, Double> unitCostByProductId = new HashMap<>();

    @FXML
    public void initialize() {
        supplierCombo.setItems(FXCollections.observableArrayList(supplierDao.findAll()));
        productCombo.setItems(FXCollections.observableArrayList(productDao.findAll()));

        lineProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        lineQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantityOrdered"));
        lineCostColumn.setCellValueFactory(new PropertyValueFactory<>("unitCost"));
        linesTable.setItems(pendingLines);
    }

    @FXML
    private void handleAddLine() {
        try {
            Product product = productCombo.getValue();
            if (product == null) {
                statusLabel.setText("Select a product first.");
                return;
            }
            int qty = Integer.parseInt(lineQuantityField.getText());
            double cost = Double.parseDouble(lineCostField.getText());

            PurchaseOrderLine line = new PurchaseOrderLine(product.getProductId(), qty, cost);
            line.setProductName(product.getName());
            pendingLines.add(line);

            lineQuantityField.clear();
            lineCostField.clear();
            statusLabel.setText("");
        } catch (NumberFormatException e) {
            statusLabel.setText("Quantity and unit cost must be numbers.");
        }
    }

    @FXML
    private void handleSubmit() {
        try {
            Supplier supplier = supplierCombo.getValue();
            if (supplier == null) {
                statusLabel.setText("Select a supplier.");
                return;
            }
            if (pendingLines.isEmpty()) {
                statusLabel.setText("Add at least one line item.");
                return;
            }

            PurchaseOrderBuilder builder = new PurchaseOrderBuilder()
                    .supplier(supplier.getSupplierId())
                    .notes(notesField.getText());
            for (PurchaseOrderLine line : pendingLines) {
                builder.addLine(line.getProductId(), line.getQuantityOrdered(), line.getUnitCost());
            }

            var po = builder.build(); // Builder validates supplier + lines
            purchaseOrderService.create(po);

            statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            statusLabel.setText("Purchase order #" + po.getPoId() + " created (status DRAFT).");
            pendingLines.clear();
            notesField.clear();
        } catch (IllegalStateException e) {
            statusLabel.setTextFill(javafx.scene.paint.Color.CRIMSON);
            statusLabel.setText(e.getMessage());
        } catch (Exception e) {
            statusLabel.setTextFill(javafx.scene.paint.Color.CRIMSON);
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
}
