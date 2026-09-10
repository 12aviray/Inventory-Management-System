package com.inventory.service;

import com.inventory.model.PurchaseOrder;
import com.inventory.model.PurchaseOrderBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderBuilderTest {

    @Test
    void builder_producesDraftOrderWithLines() {
        PurchaseOrder po = new PurchaseOrderBuilder()
                .supplier(1)
                .notes("Urgent restock")
                .addLine(1, 50, 12.50)
                .addLine(2, 100, 4.25)
                .build();

        assertEquals("DRAFT", po.getStatus());
        assertEquals(2, po.getLines().size());
        assertEquals(1, po.getSupplierId());
    }

    @Test
    void builder_rejectsMissingSupplier() {
        PurchaseOrderBuilder builder = new PurchaseOrderBuilder().addLine(1, 10, 5.0);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void builder_rejectsEmptyLines() {
        PurchaseOrderBuilder builder = new PurchaseOrderBuilder().supplier(1);
        assertThrows(IllegalStateException.class, builder::build);
    }
}
