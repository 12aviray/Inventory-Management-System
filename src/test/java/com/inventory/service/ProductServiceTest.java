package com.inventory.service;

import com.inventory.model.Product;
import com.inventory.util.DatabaseSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ProductServiceTest {

    private final ProductService productService = new ProductService();

    @BeforeEach
    void setUp() {
        DatabaseSeeder.run();
    }

    @Test
    void testFindAllReturnsSeededProducts() {
        List<Product> products = productService.findAll();
        assertNotNull(products);
        assertTrue(products.size() >= 5);
    }

    @Test
    void testCreateFindByIdAndUpdate() {
        String uniqueSku = "SKU-TEST-" + System.nanoTime();
        Product p = new Product();
        p.setSku(uniqueSku);
        p.setName("Ergonomic Keyboard");
        p.setCategory("Electronics");
        p.setUnitCost(75.50);
        p.setReorderThreshold(12);

        Product created = productService.create(p);
        assertTrue(created.getProductId() > 0);

        Optional<Product> found = productService.findById(created.getProductId());
        assertTrue(found.isPresent());
        assertEquals("Ergonomic Keyboard", found.get().getName());
        assertEquals(75.50, found.get().getUnitCost(), 0.001);

        // Update
        found.get().setName("Ergonomic Mechanical Keyboard");
        found.get().setUnitCost(89.00);
        productService.update(found.get());

        Optional<Product> updated = productService.findById(created.getProductId());
        assertTrue(updated.isPresent());
        assertEquals("Ergonomic Mechanical Keyboard", updated.get().getName());
        assertEquals(89.00, updated.get().getUnitCost(), 0.001);
    }

    @Test
    void testValidationOnCreate() {
        Product invalid = new Product();
        invalid.setSku("");
        invalid.setName("Test");
        invalid.setCategory("Test");
        invalid.setUnitCost(10.0);
        invalid.setReorderThreshold(5);

        assertThrows(IllegalArgumentException.class, () -> productService.create(invalid));

        invalid.setSku("SKU-VAL-1");
        invalid.setUnitCost(-5.0);
        assertThrows(IllegalArgumentException.class, () -> productService.create(invalid));

        invalid.setUnitCost(10.0);
        invalid.setReorderThreshold(-2);
        assertThrows(IllegalArgumentException.class, () -> productService.create(invalid));
    }
}
