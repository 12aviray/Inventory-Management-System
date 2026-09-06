PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS product (
    product_id INTEGER PRIMARY KEY AUTOINCREMENT,
    sku TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    unit_cost REAL NOT NULL CHECK (unit_cost >= 0),
    reorder_threshold INTEGER NOT NULL DEFAULT 10 CHECK (reorder_threshold >= 0),
    created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
);

CREATE TABLE IF NOT EXISTS warehouse (
    warehouse_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    location TEXT
);

CREATE TABLE IF NOT EXISTS stock_item (
    stock_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL REFERENCES product(product_id) ON DELETE CASCADE,
    warehouse_id INTEGER NOT NULL REFERENCES warehouse(warehouse_id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
    UNIQUE (product_id, warehouse_id)
);

CREATE TABLE IF NOT EXISTS supplier (
    supplier_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    contact_email TEXT,
    phone TEXT
);

CREATE TABLE IF NOT EXISTS purchase_order (
    po_id INTEGER PRIMARY KEY AUTOINCREMENT,
    supplier_id INTEGER NOT NULL REFERENCES supplier(supplier_id),
    status TEXT NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT','SENT','PARTIALLY_RECEIVED','RECEIVED','CANCELLED')),
    created_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
    expected_date INTEGER,
    notes TEXT
);

CREATE TABLE IF NOT EXISTS purchase_order_line (
    po_line_id INTEGER PRIMARY KEY AUTOINCREMENT,
    po_id INTEGER NOT NULL REFERENCES purchase_order(po_id) ON DELETE CASCADE,
    product_id INTEGER NOT NULL REFERENCES product(product_id),
    quantity_ordered INTEGER NOT NULL CHECK (quantity_ordered > 0),
    quantity_received INTEGER NOT NULL DEFAULT 0 CHECK (quantity_received >= 0),
    unit_cost REAL NOT NULL CHECK (unit_cost >= 0)
);

CREATE TABLE IF NOT EXISTS stock_movement (
    movement_id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL REFERENCES product(product_id),
    warehouse_id INTEGER NOT NULL REFERENCES warehouse(warehouse_id),
    po_id INTEGER REFERENCES purchase_order(po_id),
    movement_type TEXT NOT NULL
        CHECK (movement_type IN ('IN','OUT','TRANSFER_OUT','TRANSFER_IN','ADJUSTMENT')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    reason TEXT,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
);

CREATE INDEX IF NOT EXISTS idx_stock_item_product ON stock_item(product_id);
CREATE INDEX IF NOT EXISTS idx_stock_item_warehouse ON stock_item(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_movement_product ON stock_movement(product_id);
CREATE INDEX IF NOT EXISTS idx_movement_warehouse ON stock_movement(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_po_status ON purchase_order(status);
