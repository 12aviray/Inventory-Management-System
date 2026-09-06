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
import javafx.scene.control.cell.TextFieldTableCell;

import java.util.HashMap;
import java.util.Map;

/**
 * Drives the State-pattern-governed receiving workflow: PurchaseOrderService
 * decides (via POState) whether the PO transitions to PARTIALLY_RECEIVED or
 * RECEIVED based on whether every line is now fully received.
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
        // Only orders that can actually receive stock (SENT / PARTIALLY_RECEIVED) are relevant here.
        var receivable = purchaseOrderService.findAll().stream()
                .filter(po -> po.getStatus().equals("SENT") || po.getStatus().equals("PARTIALLY_RECEIVED"))
                .toList();
        poCombo.setItems(FXCollections.observableArrayList(receivable));
        warehouseCombo.setItems(FXCollections.observableArrayList(warehouseDao.findAll()));

        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        orderedColumn.setCellValueFactory(new PropertyValueFactory<>("quantityOrdered"));
        receivedColumn.setCellValueFactory(new PropertyValueFactory<>("quantityAlreadyReceived"));
        outstandingColumn.setCellValueFactory(new PropertyValueFactory<>("outstanding"));

        receiveNowColumn.setCellValueFactory(new PropertyValueFactory<>("receiveNow"));
        receiveNowColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        receiveNowColumn.setOnEditCommit(event ->
                event.getRowValue().setReceiveNow(event.getNewValue()));
        linesTable.setEditable(true);

        linesTable.setItems(rows);
    }

    @FXML
    private void handleLoad() {
        PurchaseOrder po = poCombo.getValue();
        if (po == null) {
            statusLabel.setText("Select a purchase order.");
            return;
        }
        rows.setAll(po.getLines().stream()
                .map(line -> new ReceiveLineRow(line.getPoLineId(), line.getProductId(),
                        line.getProductName(), line.getQuantityOrdered(), line.getQuantityReceived()))
                .toList());
        statusLabel.setText("");
    }

    @FXML
    private void handleConfirm() {
        PurchaseOrder po = poCombo.getValue();
        Warehouse warehouse = warehouseCombo.getValue();
        if (po == null || warehouse == null) {
            statusLabel.setText("Select a purchase order and receiving warehouse.");
            return;
        }

        try {
            Map<Integer, Integer> receivedQuantities = new HashMap<>();
            for (ReceiveLineRow row : rows) {
                int qty = Integer.parseInt(row.getReceiveNow().isBlank() ? "0" : row.getReceiveNow());
                if (qty > row.getOutstanding()) {
                    throw new IllegalArgumentException(
                            "Cannot receive more than outstanding quantity for " + row.getProductName());
                }
                receivedQuantities.put(row.getPoLineId(), qty);
            }

            purchaseOrderService.receiveShipment(po.getPoId(), warehouse.getWarehouseId(), receivedQuantities);

            statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            statusLabel.setText("Shipment recorded. PO status updated.");
            rows.clear();
        } catch (NumberFormatException e) {
            statusLabel.setTextFill(javafx.scene.paint.Color.CRIMSON);
            statusLabel.setText("Receive quantities must be whole numbers.");
        } catch (Exception e) {
            statusLabel.setTextFill(javafx.scene.paint.Color.CRIMSON);
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
}
