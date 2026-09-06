package com.inventory.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/** Simple sidebar navigation shell: swaps the center content between the app's screens. */
public class MainLayoutController {

    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        showDashboard();
    }

    @FXML private void showDashboard() { load("/fxml/Dashboard.fxml"); }
    @FXML private void showProducts() { load("/fxml/ProductList.fxml"); }
    @FXML private void showWarehouseStock() { load("/fxml/WarehouseStock.fxml"); }
    @FXML private void showPurchaseOrderCreate() { load("/fxml/PurchaseOrderCreate.fxml"); }
    @FXML private void showReceiveShipment() { load("/fxml/ReceiveShipment.fxml"); }
    @FXML private void showTransfer() { load("/fxml/Transfer.fxml"); }
    @FXML private void showReports() { load("/fxml/Reports.fxml"); }

    private void load(String fxmlPath) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load screen: " + fxmlPath, e);
        }
    }
}
