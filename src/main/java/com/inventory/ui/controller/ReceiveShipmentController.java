package com.inventory.ui.controller;

import com.inventory.dao.WarehouseDao;
import com.inventory.model.PurchaseOrder;
import com.inventory.model.Warehouse;
import com.inventory.service.PurchaseOrderService;
import com.inventory.ui.viewmodel.ReceiveLineRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for receiving shipments against purchase orders.
 * Follows the State pattern: updates PO state from SENT -> PARTIALLY_RECEIVED -> RECEIVED.
 * Uses Command pattern (ReceiveStockCommand) to increment stock and log stock movements.
 */
public class ReceiveShipmentController {

    @FXML private ComboBox<PurchaseOrder> poCombo;
    @FXML private ComboBox<Warehouse> warehouseCombo;

    @FXML private TableView<ReceiveLineRow> linesTable;
    @FXML private TableColumn<ReceiveLineRow, String> productColumn;
    @FXML private TableColumn<ReceiveLineRow, Integer> orderedColumn;
    @FXML private TableColumn<ReceiveLineRow, Integer> receivedColumn;
    @FXML private TableColumn<ReceiveLineRow, Integer> outstandingColumn;
    @FXML private TableColumn<ReceiveLineRow, String> receiveNowColumn;

    @FXML private Label statusLabel;

    private final PurchaseOrderService purchaseOrderService = new PurchaseOrderService();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final ObservableList<ReceiveLineRow> rows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        warehouseCombo.setItems(FXCollections.observableArrayList(warehouseDao.findAll()));

        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        orderedColumn.setCellValueFactory(new PropertyValueFactory<>("quantityOrdered"));
        receivedColumn.setCellValueFactory(new PropertyValueFactory<>("quantityAlreadyReceived"));
        outstandingColumn.setCellValueFactory(new PropertyValueFactory<>("outstanding"));

        receiveNowColumn.setCellValueFactory(new PropertyValueFactory<>("receiveNow"));
        // Custom TableCell that auto-syncs on text changes, focus loss, and enter key
        receiveNowColumn.setCellFactory(col -> new TableCell<>() {
            private final TextField textField = new TextField();

            {
                textField.getStyleClass().add("table-text-field");
                textField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (isEditing() || getTableRow() != null) {
                        ReceiveLineRow row = getTableRow().getItem();
                        if (row != null) {
                            row.setReceiveNow(newVal == null ? "" : newVal.trim());
                        }
                    }
                });
                textField.focusedProperty().addListener((obs, oldVal, isFocused) -> {
                    if (!isFocused && getTableRow() != null) {
                        ReceiveLineRow row = getTableRow().getItem();
                        if (row != null) {
                            row.setReceiveNow(textField.getText() == null ? "0" : textField.getText().trim());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    ReceiveLineRow row = getTableRow().getItem();
                    if (!textField.isFocused()) {
                        textField.setText(row.getReceiveNow());
                    }
                    setGraphic(textField);
                    setText(null);
                }
            }
        });

        linesTable.setItems(rows);

        // Auto-load lines when PO selection changes
        poCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selectedPO) -> {
            loadLinesForPO(selectedPO);
        });

        handleRefreshPOs();
    }

    @FXML
    public void handleRefreshPOs() {
        PurchaseOrder prev = poCombo.getValue();
        int prevId = prev != null ? prev.getPoId() : -1;

        List<PurchaseOrder> receivable = purchaseOrderService.findAll().stream()
                .filter(po -> "SENT".equals(po.getStatus()) || "PARTIALLY_RECEIVED".equals(po.getStatus()))
                .toList();

        poCombo.setItems(FXCollections.observableArrayList(receivable));

        if (prevId != -1) {
            for (PurchaseOrder po : receivable) {
                if (po.getPoId() == prevId) {
                    poCombo.getSelectionModel().select(po);
                    return;
                }
            }
        }

        if (!receivable.isEmpty() && poCombo.getValue() == null) {
            poCombo.getSelectionModel().selectFirst();
        } else if (receivable.isEmpty()) {
            rows.clear();
            showStatus("No approved purchase orders (SENT or PARTIALLY_RECEIVED) available to receive.", false);
        }
    }

    private void loadLinesForPO(PurchaseOrder po) {
        if (po == null) {
            rows.clear();
            return;
        }

        // Fetch fresh PO with lines
        PurchaseOrder freshPO = purchaseOrderService.findAll().stream()
                .filter(p -> p.getPoId() == po.getPoId())
                .findFirst()
                .orElse(po);

        rows.setAll(freshPO.getLines().stream()
                .map(line -> {
                    ReceiveLineRow row = new ReceiveLineRow(line.getPoLineId(), line.getProductId(),
                            line.getProductName(), line.getQuantityOrdered(), line.getQuantityReceived());
                    row.setReceiveNow(String.valueOf(line.getOutstandingQuantity()));
                    return row;
                })
                .toList());

        statusLabel.setText("");
    }

    @FXML
    private void handleFillAllOutstanding() {
        for (ReceiveLineRow row : rows) {
            row.setReceiveNow(String.valueOf(row.getOutstanding()));
        }
        linesTable.refresh();
        showStatus("Set quantities to all remaining outstanding items.", false);
    }

    @FXML
    private void handleClearQuantities() {
        for (ReceiveLineRow row : rows) {
            row.setReceiveNow("0");
        }
        linesTable.refresh();
        showStatus("Cleared receive quantities.", false);
    }

    @FXML
    private void handleConfirm() {
        PurchaseOrder po = poCombo.getValue();
        Warehouse warehouse = warehouseCombo.getValue();
        if (po == null) {
            showStatus("Please select a purchase order to receive.", true);
            return;
        }
        if (warehouse == null) {
            showStatus("Please select a destination receiving warehouse.", true);
            return;
        }
        if (rows.isEmpty()) {
            showStatus("No line items available to receive for this order.", true);
            return;
        }

        try {
            Map<Integer, Integer> receivedQuantities = new HashMap<>();
            int totalReceiving = 0;

            for (ReceiveLineRow row : rows) {
                String input = row.getReceiveNow() == null ? "0" : row.getReceiveNow().trim();
                int qty = input.isBlank() ? 0 : Integer.parseInt(input);

                if (qty < 0) {
                    showStatus("Receive quantity cannot be negative for " + row.getProductName(), true);
                    return;
                }
                if (qty > row.getOutstanding()) {
                    showStatus("Cannot receive more than outstanding quantity (" + row.getOutstanding() + ") for " + row.getProductName(), true);
                    return;
                }
                receivedQuantities.put(row.getPoLineId(), qty);
                totalReceiving += qty;
            }

            if (totalReceiving == 0) {
                showStatus("Total receive quantity is 0. Please enter at least 1 item to receive.", true);
                return;
            }

            purchaseOrderService.receiveShipment(po.getPoId(), warehouse.getWarehouseId(), receivedQuantities);

            showStatus("Successfully recorded receipt of " + totalReceiving + " units into " + warehouse.getName() + "!", false);

            handleRefreshPOs();
        } catch (NumberFormatException e) {
            showStatus("Receive quantities must be valid whole numbers.", true);
        } catch (Exception e) {
            showStatus("Error recording shipment: " + e.getMessage(), true);
        }
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setStyle(isError ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;" : "-fx-text-fill: #10b981; -fx-font-weight: bold;");
        statusLabel.setText(message);
    }
}
