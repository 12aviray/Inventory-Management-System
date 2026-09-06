package com.inventory.model;

/**
 * Builder pattern.
 *
 * Problem: PurchaseOrder has one required field (supplier) plus several optional
 * ones (expected date, notes) and a variable-length list of lines that are added
 * incrementally as the user fills out the "create purchase order" screen. A
 * telescoping constructor (or one giant constructor with many parameters) would be
 * error-prone and hard to read at call sites, especially since lines are added one
 * row at a time from the UI, not all at once.
 *
 * Why Builder: it lets the UI controller add lines and set optional fields
 * incrementally, validates before final construction, and keeps PurchaseOrder
 * itself immutable-ish/consistent once built (status always starts at DRAFT).
 *
 * Alternative considered: a plain setter-based POJO (as used for simpler entities
 * like Product). Rejected here because construction of a PO is itself a multi-step
 * process with validation at the end, which is exactly what Builder is for -
 * simpler entities don't have that need, so they don't get one.
 */
public class PurchaseOrderBuilder {

    private final PurchaseOrder purchaseOrder = new PurchaseOrder();

    public PurchaseOrderBuilder supplier(int supplierId) {
        purchaseOrder.setSupplierId(supplierId);
        return this;
    }

    public PurchaseOrderBuilder expectedDate(long epochSeconds) {
        purchaseOrder.setExpectedDate(epochSeconds);
        return this;
    }

    public PurchaseOrderBuilder notes(String notes) {
        purchaseOrder.setNotes(notes);
        return this;
    }

    public PurchaseOrderBuilder addLine(int productId, int quantity, double unitCost) {
        purchaseOrder.addLine(new PurchaseOrderLine(productId, quantity, unitCost));
        return this;
    }

    public PurchaseOrder build() {
        if (purchaseOrder.getSupplierId() <= 0) {
            throw new IllegalStateException("A purchase order requires a supplier");
        }
        if (purchaseOrder.getLines().isEmpty()) {
            throw new IllegalStateException("A purchase order requires at least one line");
        }
        purchaseOrder.setStatus("DRAFT");
        return purchaseOrder;
    }
}
