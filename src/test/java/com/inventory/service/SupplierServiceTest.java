package com.inventory.service;

import com.inventory.model.Supplier;
import com.inventory.util.DatabaseSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SupplierServiceTest {

    private final SupplierService supplierService = new SupplierService();

    @BeforeEach
    void setUp() {
        DatabaseSeeder.run();
    }

    @Test
    void testCreateFindUpdateAndDelete() {
        Supplier s = new Supplier();
        s.setName("Test Supplier " + System.nanoTime());
        s.setContactEmail("orders@testsupplier.com");
        s.setPhone("+880-1700-000000");

        Supplier created = supplierService.create(s);
        assertTrue(created.getSupplierId() > 0);

        Optional<Supplier> found = supplierService.findById(created.getSupplierId());
        assertTrue(found.isPresent());
        assertEquals("orders@testsupplier.com", found.get().getContactEmail());

        found.get().setPhone("+880-1800-111111");
        supplierService.update(found.get());

        Optional<Supplier> updated = supplierService.findById(created.getSupplierId());
        assertTrue(updated.isPresent());
        assertEquals("+880-1800-111111", updated.get().getPhone());

        supplierService.delete(created.getSupplierId());
        assertTrue(supplierService.findById(created.getSupplierId()).isEmpty());
    }

    @Test
    void testValidationOnCreate() {
        Supplier invalid = new Supplier();
        invalid.setName("");
        assertThrows(IllegalArgumentException.class, () -> supplierService.create(invalid));
    }

    @Test
    void testValidationRejectsMalformedEmail() {
        Supplier invalid = new Supplier();
        invalid.setName("Some Supplier " + System.nanoTime());
        invalid.setContactEmail("not-an-email");

        assertThrows(IllegalArgumentException.class, () -> supplierService.create(invalid));
    }
}
