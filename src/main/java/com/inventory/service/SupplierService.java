package com.inventory.service;

import com.inventory.dao.SupplierDao;
import com.inventory.model.Supplier;

import java.util.List;
import java.util.Optional;

/**
 * Thin service layer over SupplierDao. Like ProductService and
 * WarehouseService, this is deliberately kept pattern-free: Supplier CRUD
 * has no meaningful business rules beyond basic validation
 * (see docs/03-design-patterns.md on avoiding patterns for pattern's sake).
 */
public class SupplierService {

    private final SupplierDao supplierDao = new SupplierDao();

    public List<Supplier> findAll() {
        return supplierDao.findAll();
    }

    public Optional<Supplier> findById(int supplierId) {
        return supplierDao.findById(supplierId);
    }

    public Supplier create(Supplier supplier) {
        validate(supplier);
        return supplierDao.insert(supplier);
    }

    public void update(Supplier supplier) {
        validate(supplier);
        supplierDao.update(supplier);
    }

    public void delete(int supplierId) {
        supplierDao.delete(supplierId);
    }

    private void validate(Supplier supplier) {
        if (supplier.getName() == null || supplier.getName().isBlank()) {
            throw new IllegalArgumentException("Supplier name is required");
        }
        String email = supplier.getContactEmail();
        if (email != null && !email.isBlank() && !email.contains("@")) {
            throw new IllegalArgumentException("Contact email must be a valid email address");
        }
    }
}
