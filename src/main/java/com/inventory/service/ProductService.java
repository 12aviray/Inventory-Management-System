package com.inventory.service;

import com.inventory.dao.ProductDao;
import com.inventory.model.Product;

import java.util.List;

/**
 * Thin service layer over ProductDao. Kept simple deliberately: Product CRUD
 * has no meaningful business rules beyond basic validation, which is exactly
 * why it does NOT get a design pattern applied to it (see docs/03-design-patterns.md
 * on avoiding patterns forced onto simple CRUD).
 */
public class ProductService {

    private final ProductDao productDao = new ProductDao();

    public List<Product> findAll() {
        return productDao.findAll();
    }

    public java.util.Optional<Product> findById(int productId) {
        return productDao.findById(productId);
    }

    public Product create(Product product) {
        validate(product);
        return productDao.insert(product);
    }

    public void update(Product product) {
        validate(product);
        productDao.update(product);
    }

    public void delete(int productId) {
        productDao.delete(productId);
    }

    private void validate(Product product) {
        if (product.getSku() == null || product.getSku().isBlank()) {
            throw new IllegalArgumentException("SKU is required");
        }
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (product.getUnitCost() < 0) {
            throw new IllegalArgumentException("Unit cost cannot be negative");
        }
        if (product.getReorderThreshold() < 0) {
            throw new IllegalArgumentException("Reorder threshold cannot be negative");
        }
    }
}
