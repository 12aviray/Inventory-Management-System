package com.inventory.ui.controller;

import com.inventory.model.Supplier;
import com.inventory.model.Warehouse;
import com.inventory.service.SupplierService;
import com.inventory.service.WarehouseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Controller for the Settings screen: CRUD management for Warehouse and
 * Supplier, the two reference entities that previously had create/read
 * only. Kept in one controller behind a TabPane (rather than two separate
 * top-level nav screens) to avoid inflating the screen count with plain
 * admin/reference-data forms.
 */
public class SettingsController {

    // --- Warehouses tab ---
    @FXML private TableView<Warehouse> warehouseTable;
    @FXML private TableColumn<Warehouse, String> warehouseNameColumn;
    @FXML private TableColumn<Warehouse, String> warehouseLocationColumn;
    @FXML private TextField warehouseNameField;
    @FXML private TextField warehouseLocationField;
    @FXML private Label warehouseStatusLabel;

    private final WarehouseService warehouseService = new WarehouseService();
    private final ObservableList<Warehouse> warehouses = FXCollections.observableArrayList();

    // --- Suppliers tab ---
    @FXML private TableView<Supplier> supplierTable;
    @FXML private TableColumn<Supplier, String> supplierNameColumn;
    @FXML private TableColumn<Supplier, String> supplierEmailColumn;
    @FXML private TableColumn<Supplier, String> supplierPhoneColumn;
    @FXML private TextField supplierNameField;
    @FXML private TextField supplierEmailField;
    @FXML private TextField supplierPhoneField;
    @FXML private Label supplierStatusLabel;

    private final SupplierService supplierService = new SupplierService();
    private final ObservableList<Supplier> suppliers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        warehouseNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        warehouseLocationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        warehouseTable.setItems(warehouses);
        warehouseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                warehouseNameField.setText(selected.getName());
                warehouseLocationField.setText(selected.getLocation());
                warehouseStatusLabel.setText("");
            }
        });

        supplierNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        supplierEmailColumn.setCellValueFactory(new PropertyValueFactory<>("contactEmail"));
        supplierPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        supplierTable.setItems(suppliers);
        supplierTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                supplierNameField.setText(selected.getName());
                supplierEmailField.setText(selected.getContactEmail());
                supplierPhoneField.setText(selected.getPhone());
                supplierStatusLabel.setText("");
            }
        });

        refreshWarehouses();
        refreshSuppliers();
    }

    // ---------------- Warehouse CRUD ----------------

    @FXML
    private void handleWarehouseRefresh() {
        refreshWarehouses();
    }

    private void refreshWarehouses() {
        Warehouse selected = warehouseTable.getSelectionModel().getSelectedItem();
        int prevId = selected != null ? selected.getWarehouseId() : -1;

        List<Warehouse> all = warehouseService.findAll();
        warehouses.setAll(all);

        if (prevId != -1) {
            for (Warehouse w : all) {
                if (w.getWarehouseId() == prevId) {
                    warehouseTable.getSelectionModel().select(w);
                    break;
                }
            }
        }
    }

    @FXML
    private void handleWarehouseAdd() {
        try {
            Warehouse w = readWarehouseForm();
            warehouseService.create(w);
            refreshWarehouses();
            handleWarehouseClearForm();
            showWarehouseStatus("Warehouse '" + w.getName() + "' created successfully!", false);
        } catch (IllegalArgumentException e) {
            showWarehouseStatus(e.getMessage(), true);
        } catch (Exception e) {
            String msg = e.getMessage() != null && e.getMessage().contains("UNIQUE")
                    ? "A warehouse named '" + warehouseNameField.getText().trim() + "' already exists."
                    : "Error creating warehouse: " + e.getMessage();
            showWarehouseStatus(msg, true);
        }
    }

    @FXML
    private void handleWarehouseUpdate() {
        Warehouse selected = warehouseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarehouseStatus("Select a warehouse from the table to update.", true);
            return;
        }
        try {
            Warehouse w = readWarehouseForm();
            w.setWarehouseId(selected.getWarehouseId());
            warehouseService.update(w);
            refreshWarehouses();
            showWarehouseStatus("Warehouse '" + w.getName() + "' updated successfully!", false);
        } catch (IllegalArgumentException e) {
            showWarehouseStatus(e.getMessage(), true);
        } catch (Exception e) {
            String msg = e.getMessage() != null && e.getMessage().contains("UNIQUE")
                    ? "A warehouse named '" + warehouseNameField.getText().trim() + "' already exists."
                    : "Error updating warehouse: " + e.getMessage();
            showWarehouseStatus(msg, true);
        }
    }

    @FXML
    private void handleWarehouseDelete() {
        Warehouse selected = warehouseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarehouseStatus("Select a warehouse from the table to delete.", true);
            return;
        }
        try {
            warehouseService.delete(selected.getWarehouseId());
            refreshWarehouses();
            handleWarehouseClearForm();
            showWarehouseStatus("Warehouse '" + selected.getName() + "' deleted successfully.", false);
        } catch (Exception e) {
            showWarehouseStatus("Cannot delete warehouse: it still has stock items, transfers, or movements referencing it.", true);
        }
    }

    @FXML
    private void handleWarehouseClearForm() {
        warehouseTable.getSelectionModel().clearSelection();
        warehouseNameField.clear();
        warehouseLocationField.clear();
        warehouseStatusLabel.setText("");
    }

    private Warehouse readWarehouseForm() {
        String name = warehouseNameField.getText() == null ? "" : warehouseNameField.getText().trim();
        String location = warehouseLocationField.getText() == null ? "" : warehouseLocationField.getText().trim();

        if (name.isEmpty()) throw new IllegalArgumentException("Warehouse name cannot be empty.");

        Warehouse w = new Warehouse();
        w.setName(name);
        w.setLocation(location.isEmpty() ? null : location);
        return w;
    }

    private void showWarehouseStatus(String message, boolean isError) {
        warehouseStatusLabel.setStyle(isError ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;" : "-fx-text-fill: #10b981; -fx-font-weight: bold;");
        warehouseStatusLabel.setText(message);
    }

    // ---------------- Supplier CRUD ----------------

    @FXML
    private void handleSupplierRefresh() {
        refreshSuppliers();
    }

    private void refreshSuppliers() {
        Supplier selected = supplierTable.getSelectionModel().getSelectedItem();
        int prevId = selected != null ? selected.getSupplierId() : -1;

        List<Supplier> all = supplierService.findAll();
        suppliers.setAll(all);

        if (prevId != -1) {
            for (Supplier s : all) {
                if (s.getSupplierId() == prevId) {
                    supplierTable.getSelectionModel().select(s);
                    break;
                }
            }
        }
    }

    @FXML
    private void handleSupplierAdd() {
        try {
            Supplier s = readSupplierForm();
            supplierService.create(s);
            refreshSuppliers();
            handleSupplierClearForm();
            showSupplierStatus("Supplier '" + s.getName() + "' created successfully!", false);
        } catch (IllegalArgumentException e) {
            showSupplierStatus(e.getMessage(), true);
        } catch (Exception e) {
            showSupplierStatus("Error creating supplier: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleSupplierUpdate() {
        Supplier selected = supplierTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showSupplierStatus("Select a supplier from the table to update.", true);
            return;
        }
        try {
            Supplier s = readSupplierForm();
            s.setSupplierId(selected.getSupplierId());
            supplierService.update(s);
            refreshSuppliers();
            showSupplierStatus("Supplier '" + s.getName() + "' updated successfully!", false);
        } catch (IllegalArgumentException e) {
            showSupplierStatus(e.getMessage(), true);
        } catch (Exception e) {
            showSupplierStatus("Error updating supplier: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleSupplierDelete() {
        Supplier selected = supplierTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showSupplierStatus("Select a supplier from the table to delete.", true);
            return;
        }
        try {
            supplierService.delete(selected.getSupplierId());
            refreshSuppliers();
            handleSupplierClearForm();
            showSupplierStatus("Supplier '" + selected.getName() + "' deleted successfully.", false);
        } catch (Exception e) {
            showSupplierStatus("Cannot delete supplier: it still has purchase orders referencing it.", true);
        }
    }

    @FXML
    private void handleSupplierClearForm() {
        supplierTable.getSelectionModel().clearSelection();
        supplierNameField.clear();
        supplierEmailField.clear();
        supplierPhoneField.clear();
        supplierStatusLabel.setText("");
    }

    private Supplier readSupplierForm() {
        String name = supplierNameField.getText() == null ? "" : supplierNameField.getText().trim();
        String email = supplierEmailField.getText() == null ? "" : supplierEmailField.getText().trim();
        String phone = supplierPhoneField.getText() == null ? "" : supplierPhoneField.getText().trim();

        if (name.isEmpty()) throw new IllegalArgumentException("Supplier name cannot be empty.");

        Supplier s = new Supplier();
        s.setName(name);
        s.setContactEmail(email.isEmpty() ? null : email);
        s.setPhone(phone.isEmpty() ? null : phone);
        return s;
    }

    private void showSupplierStatus(String message, boolean isError) {
        supplierStatusLabel.setStyle(isError ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;" : "-fx-text-fill: #10b981; -fx-font-weight: bold;");
        supplierStatusLabel.setText(message);
    }
}
