package com.inventory.service;

import com.inventory.model.Warehouse;
import com.inventory.util.DatabaseSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class WarehouseServiceTest {

    private final WarehouseService warehouseService = new WarehouseService();

    @BeforeEach
    void setUp() {
        DatabaseSeeder.run();
    }

    @Test
    void testCreateFindUpdateAndDelete() {
        Warehouse w = new Warehouse();
        w.setName("Test Depot " + System.nanoTime());
        w.setLocation("Rajshahi, Bangladesh");

        Warehouse created = warehouseService.create(w);
        assertTrue(created.getWarehouseId() > 0);

        Optional<Warehouse> found = warehouseService.findById(created.getWarehouseId());
        assertTrue(found.isPresent());
        assertEquals("Rajshahi, Bangladesh", found.get().getLocation());

        found.get().setLocation("Khulna, Bangladesh");
        warehouseService.update(found.get());

        Optional<Warehouse> updated = warehouseService.findById(created.getWarehouseId());
        assertTrue(updated.isPresent());
        assertEquals("Khulna, Bangladesh", updated.get().getLocation());

        warehouseService.delete(created.getWarehouseId());
        assertTrue(warehouseService.findById(created.getWarehouseId()).isEmpty());
    }

    @Test
    void testValidationOnCreate() {
        Warehouse invalid = new Warehouse();
        invalid.setName("");
        invalid.setLocation("Somewhere");

        assertThrows(IllegalArgumentException.class, () -> warehouseService.create(invalid));
    }

    @Test
    void testValidationOnUpdate() {
        Warehouse w = new Warehouse();
        w.setName("Temp Warehouse " + System.nanoTime());
        Warehouse created = warehouseService.create(w);

        created.setName("");
        assertThrows(IllegalArgumentException.class, () -> warehouseService.update(created));
    }
}
