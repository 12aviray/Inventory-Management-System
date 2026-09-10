package com.inventory.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;

/**
 * Main application shell controller: manages navigation between views and active button styling.
 */
public class MainLayoutController {

    @FXML private StackPane contentArea;

    @FXML private Button btnDashboard;
    @FXML private Button btnProducts;
    @FXML private Button btnStock;
    @FXML private Button btnPurchaseOrders;
    @FXML private Button btnReceive;
    @FXML private Button btnSales;
    @FXML private Button btnTransfer;
    @FXML private Button btnReports;
    @FXML private Button btnSettings;

    private List<Button> navButtons;

    @FXML
    public void initialize() {
        navButtons = List.of(btnDashboard, btnProducts, btnStock, btnPurchaseOrders, btnReceive, btnSales, btnTransfer, btnReports, btnSettings);
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        setActiveButton(btnDashboard);
        load("/fxml/Dashboard.fxml");
    }

    @FXML
    public void showProducts() {
        setActiveButton(btnProducts);
        load("/fxml/ProductList.fxml");
    }

    @FXML
    public void showWarehouseStock() {
        setActiveButton(btnStock);
        load("/fxml/WarehouseStock.fxml");
    }

    @FXML
    public void showPurchaseOrderCreate() {
        setActiveButton(btnPurchaseOrders);
        load("/fxml/PurchaseOrderCreate.fxml");
    }

    @FXML
    public void showReceiveShipment() {
        setActiveButton(btnReceive);
        load("/fxml/ReceiveShipment.fxml");
    }

    @FXML
    public void showSales() {
        setActiveButton(btnSales);
        load("/fxml/Sales.fxml");
    }

    @FXML
    public void showTransfer() {
        setActiveButton(btnTransfer);
        load("/fxml/Transfer.fxml");
    }

    @FXML
    public void showReports() {
        setActiveButton(btnReports);
        load("/fxml/Reports.fxml");
    }

    @FXML
    public void showSettings() {
        setActiveButton(btnSettings);
        load("/fxml/Settings.fxml");
    }

    private void setActiveButton(Button active) {
        if (navButtons == null) return;
        for (Button btn : navButtons) {
            if (btn != null) {
                if (btn == active) {
                    if (!btn.getStyleClass().contains("nav-btn-active")) {
                        btn.getStyleClass().add("nav-btn-active");
                    }
                } else {
                    btn.getStyleClass().remove("nav-btn-active");
                }
            }
        }
    }

    private void load(String fxmlPath) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load screen: " + fxmlPath, e);
        }
    }
}
