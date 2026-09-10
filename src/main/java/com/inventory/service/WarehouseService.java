package com.inventory.service;

import com.inventory.dao.WarehouseDao;
import com.inventory.model.Warehouse;

import java.util.List;
import java.util.Optional;

/**
 * Thin service layer over WarehouseDao. Like ProductService, this is
 * deliberately kept pattern-free: Warehouse CRUD has no meaningful business
 * rules beyond basic validation, so no design pattern is forced onto it
 * (see docs/03-design-patterns.md on avoiding patterns for pattern's sake).
 */
public class WarehouseService {

    private final WarehouseDao warehouseDao = new WarehouseDao();

    public List<Warehouse> findAll() {
        return warehouseDao.findAll();
    }

    public Optional<Warehouse> findById(int warehouseId) {
        return warehouseDao.findById(warehouseId);
    }

    public Warehouse create(Warehouse warehouse) {
        validate(warehouse);
        return warehouseDao.insert(warehouse);
    }

    public void update(Warehouse warehouse) {
        validate(warehouse);
        warehouseDao.update(warehouse);
    }

    public void delete(int warehouseId) {
        warehouseDao.delete(warehouseId);
    }

    private void validate(Warehouse warehouse) {
        if (warehouse.getName() == null || warehouse.getName().isBlank()) {
            throw new IllegalArgumentException("Warehouse name is required");
        }
    }
}
