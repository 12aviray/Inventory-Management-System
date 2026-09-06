package com.inventory.model;

import java.util.ArrayList;
import java.util.List;

public class PurchaseOrder {
    private int poId;
    private int supplierId;
    private String supplierName; // convenience, from join
    private String status; // DRAFT, SENT, PARTIALLY_RECEIVED, RECEIVED, CANCELLED
    private long expectedDate;
    private String notes;
    private final List<PurchaseOrderLine> lines = new ArrayList<>();

    public PurchaseOrder() {
    }

    public int getPoId() { return poId; }
    public void setPoId(int poId) { this.poId = poId; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getExpectedDate() { return expectedDate; }
    public void setExpectedDate(long expectedDate) { this.expectedDate = expectedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<PurchaseOrderLine> getLines() { return lines; }

    public void addLine(PurchaseOrderLine line) { lines.add(line); }

    @Override
    public String toString() {
        return "PO #" + poId + " - " + supplierName + " (" + status + ")";
    }
}
