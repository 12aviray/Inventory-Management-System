package com.inventory.ui.controller;

import com.inventory.dao.ProductDao;
import com.inventory.dao.SupplierDao;
import com.inventory.model.Product;
import com.inventory.model.PurchaseOrder;
import com.inventory.model.PurchaseOrderBuilder;
import com.inventory.model.PurchaseOrderLine;
import com.inventory.model.Supplier;
import com.inventory.service.PurchaseOrderService;
import com.inventory.service.state.POState;
import com.inventory.service.state.POStateFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Purchase Order creation (Builder pattern) and Purchase Order lifecycle
 * management (State pattern: DRAFT -> SENT -> PARTIALLY_RECEIVED -> RECEIVED / CANCELLED).
 */
public class PurchaseOrderCreateController {

    // Tab 1: Create Order
    @FXML private TabPane poTabPane;
    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private DatePicker expectedDatePicker;
    @FXML private TextField notesField;
    @FXML private ComboBox<Product> productCombo;
    @FXML private TextField lineQuantityField;
    @FXML private TextField lineCostField;

    @FXML private TableView<PurchaseOrderLine> linesTable;
    @FXML private TableColumn<PurchaseOrderLine, String> lineProductColumn;
    @FXML private TableColumn<PurchaseOrderLine, Integer> lineQuantityColumn;
    @FXML private TableColumn<PurchaseOrderLine, Double> lineCostColumn;
    @FXML private TableColumn<PurchaseOrderLine, Double> lineTotalCostColumn;

    @FXML private Label statusLabel;

    // Tab 2: Manage Orders
    @FXML private TableView<PurchaseOrder> allOrdersTable;
    @FXML private TableColumn<PurchaseOrder, Integer> orderIdCol;
    @FXML private TableColumn<PurchaseOrder, String> orderSupplierCol;
    @FXML private TableColumn<PurchaseOrder, String> orderStatusCol;
    @FXML private TableColumn<PurchaseOrder, String> orderExpectedCol;
    @FXML private TableColumn<PurchaseOrder, String> orderNotesCol;

    @FXML private Label selectedOrderHeaderLabel;
    @FXML private Button sendOrderBtn;
    @FXML private Button cancelOrderBtn;
    @FXML private Label manageStatusLabel;

    @FXML private TableView<PurchaseOrderLine> selectedOrderLinesTable;
    @FXML private TableColumn<PurchaseOrderLine, String> detailProductCol;
    @FXML private TableColumn<PurchaseOrderLine, Integer> detailOrderedCol;
    @FXML private TableColumn<PurchaseOrderLine, Integer> detailReceivedCol;
    @FXML private TableColumn<PurchaseOrderLine, Integer> detailOutstandingCol;
    @FXML private TableColumn<PurchaseOrderLine, Double> detailCostCol;

    private final SupplierDao supplierDao = new SupplierDao();
    private final ProductDao productDao = new ProductDao();
    private final PurchaseOrderService purchaseOrderService = new PurchaseOrderService();

    private final ObservableList<PurchaseOrderLine> pendingLines = FXCollections.observableArrayList();
    private final ObservableList<PurchaseOrder> allOrders = FXCollections.observableArrayList();
    private final ObservableList<PurchaseOrderLine> selectedOrderLines = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        // --- Setup Tab 1 (Create Order) ---
        supplierCombo.setItems(FXCollections.observableArrayList(supplierDao.findAll()));
        List<Product> products = productDao.findAll();
        productCombo.setItems(FXCollections.observableArrayList(products));

        // Auto-populate unit cost when a product is selected
        productCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lineCostField.setText(String.format("%.2f", newVal.getUnitCost()));
            }
        });

        lineProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        lineQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantityOrdered"));
        lineCostColumn.setCellValueFactory(new PropertyValueFactory<>("unitCost"));
        lineTotalCostColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        linesTable.setItems(pendingLines);

        // --- Setup Tab 2 (Manage Orders) ---
        orderIdCol.setCellValueFactory(new PropertyValueFactory<>("poId"));
        orderSupplierCol.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        orderStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        orderExpectedCol.setCellValueFactory(cellData -> {
            long ep = cellData.getValue().getExpectedDate();
            if (ep <= 0) return new SimpleStringProperty("-");
            return new SimpleStringProperty(DATE_FORMATTER.format(
                    Instant.ofEpochSecond(ep).atZone(ZoneId.systemDefault()).toLocalDate()));
        });
        orderNotesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
        allOrdersTable.setItems(allOrders);

        // Setup Details Table
        detailProductCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        detailOrderedCol.setCellValueFactory(new PropertyValueFactory<>("quantityOrdered"));
        detailReceivedCol.setCellValueFactory(new PropertyValueFactory<>("quantityReceived"));
        detailOutstandingCol.setCellValueFactory(new PropertyValueFactory<>("outstandingQuantity"));
        detailCostCol.setCellValueFactory(new PropertyValueFactory<>("unitCost"));
        selectedOrderLinesTable.setItems(selectedOrderLines);

        allOrdersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selectedPO) -> {
            updateSelectedOrderView(selectedPO);
        });

        handleRefreshOrders();
    }

    @FXML
    private void handleAddLine() {
        try {
            Product product = productCombo.getValue();
            if (product == null) {
                showStatus(statusLabel, "Please select a product first.", true);
                return;
            }
            int qty = Integer.parseInt(lineQuantityField.getText().trim());
            if (qty <= 0) {
                showStatus(statusLabel, "Quantity must be greater than 0.", true);
                return;
            }
            double cost = Double.parseDouble(lineCostField.getText().trim());
            if (cost < 0) {
                showStatus(statusLabel, "Unit cost cannot be negative.", true);
                return;
            }

            PurchaseOrderLine line = new PurchaseOrderLine(product.getProductId(), qty, cost);
            line.setProductName(product.getName());
            pendingLines.add(line);

            lineQuantityField.clear();
            showStatus(statusLabel, "Added " + product.getName() + " (Qty: " + qty + ") to order.", false);
        } catch (NumberFormatException e) {
            showStatus(statusLabel, "Quantity and unit cost must be valid numbers.", true);
        }
    }

    @FXML
    private void handleRemoveLine() {
        PurchaseOrderLine selected = linesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus(statusLabel, "Select a line item from the table to remove.", true);
            return;
        }
        pendingLines.remove(selected);
        showStatus(statusLabel, "Removed line item.", false);
    }

    @FXML
    private void handleSubmitDraft() {
        submitOrder(false);
    }

    @FXML
    private void handleSubmitAndSend() {
        submitOrder(true);
    }

    private void submitOrder(boolean andSend) {
        try {
            Supplier supplier = supplierCombo.getValue();
            if (supplier == null) {
                showStatus(statusLabel, "Please select a supplier.", true);
                return;
            }
            if (pendingLines.isEmpty()) {
                showStatus(statusLabel, "Please add at least one line item to the order.", true);
                return;
            }

            PurchaseOrderBuilder builder = new PurchaseOrderBuilder()
                    .supplier(supplier.getSupplierId())
                    .notes(notesField.getText() == null ? "" : notesField.getText().trim());

            if (expectedDatePicker.getValue() != null) {
                long epoch = expectedDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
                builder.expectedDate(epoch);
            }

            for (PurchaseOrderLine line : pendingLines) {
                builder.addLine(line.getProductId(), line.getQuantityOrdered(), line.getUnitCost());
            }

            PurchaseOrder po = builder.build();
            purchaseOrderService.create(po);

            if (andSend) {
                purchaseOrderService.send(po.getPoId());
                showStatus(statusLabel, "Purchase Order #" + po.getPoId() + " created and SENT to supplier!", false);
            } else {
                showStatus(statusLabel, "Purchase Order #" + po.getPoId() + " created as DRAFT.", false);
            }

            handleClearForm();
            handleRefreshOrders();
        } catch (IllegalStateException e) {
            showStatus(statusLabel, e.getMessage(), true);
        } catch (Exception e) {
            showStatus(statusLabel, "Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleClearForm() {
        pendingLines.clear();
        notesField.clear();
        lineQuantityField.clear();
        expectedDatePicker.setValue(null);
        productCombo.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRefreshOrders() {
        PurchaseOrder previouslySelected = allOrdersTable.getSelectionModel().getSelectedItem();
        int prevId = previouslySelected != null ? previouslySelected.getPoId() : -1;

        allOrders.setAll(purchaseOrderService.findAll());

        if (prevId != -1) {
            for (PurchaseOrder po : allOrders) {
                if (po.getPoId() == prevId) {
                    allOrdersTable.getSelectionModel().select(po);
                    break;
                }
            }
        }
    }

    private void updateSelectedOrderView(PurchaseOrder po) {
        if (po == null) {
            selectedOrderHeaderLabel.setText("Select a purchase order on the left to view details.");
            sendOrderBtn.setDisable(true);
            cancelOrderBtn.setDisable(true);
            selectedOrderLines.clear();
            manageStatusLabel.setText("");
            return;
        }

        POState state = POStateFactory.fromStatus(po.getStatus());
        selectedOrderHeaderLabel.setText(String.format("PO #%d | Supplier: %s | Status: %s",
                po.getPoId(), po.getSupplierName(), po.getStatus()));

        sendOrderBtn.setDisable(!state.canSend());
        cancelOrderBtn.setDisable(!state.canCancel());
        selectedOrderLines.setAll(po.getLines());
        manageStatusLabel.setText("");
    }

    @FXML
    private void handleSendSelected() {
        PurchaseOrder selected = allOrdersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            purchaseOrderService.send(selected.getPoId());
            showStatus(manageStatusLabel, "PO #" + selected.getPoId() + " has been SENT to supplier!", false);
            handleRefreshOrders();
        } catch (Exception e) {
            showStatus(manageStatusLabel, "Error sending order: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleCancelSelected() {
        PurchaseOrder selected = allOrdersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            purchaseOrderService.cancel(selected.getPoId());
            showStatus(manageStatusLabel, "PO #" + selected.getPoId() + " has been CANCELLED.", false);
            handleRefreshOrders();
        } catch (Exception e) {
            showStatus(manageStatusLabel, "Error cancelling order: " + e.getMessage(), true);
        }
    }

    private void showStatus(Label label, String message, boolean isError) {
        label.setStyle(isError ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;" : "-fx-text-fill: #10b981; -fx-font-weight: bold;");
        label.setText(message);
    }
}
