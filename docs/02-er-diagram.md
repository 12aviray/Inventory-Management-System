# ER Diagram

```mermaid
erDiagram
    PRODUCT ||--o{ STOCK_ITEM : "stocked as"
    WAREHOUSE ||--o{ STOCK_ITEM : "holds"
    PRODUCT ||--o{ PURCHASE_ORDER_LINE : "ordered as"
    SUPPLIER ||--o{ PURCHASE_ORDER : "supplies"
    PURCHASE_ORDER ||--o{ PURCHASE_ORDER_LINE : "contains"
    PRODUCT ||--o{ STOCK_MOVEMENT : "tracked in"
    WAREHOUSE ||--o{ STOCK_MOVEMENT : "occurs at"
    PURCHASE_ORDER ||--o{ STOCK_MOVEMENT : "triggers (nullable)"

    PRODUCT {
        int product_id PK
        string sku
        string name
        string category
        decimal unit_cost
        int reorder_threshold
        int created_at
    }
    WAREHOUSE {
        int warehouse_id PK
        string name
        string location
    }
    STOCK_ITEM {
        int stock_item_id PK
        int product_id FK
        int warehouse_id FK
        int quantity
        int updated_at
    }
    SUPPLIER {
        int supplier_id PK
        string name
        string contact_email
        string phone
    }
    PURCHASE_ORDER {
        int po_id PK
        int supplier_id FK
        string status
        int created_at
        int expected_date
        string notes
    }
    PURCHASE_ORDER_LINE {
        int po_line_id PK
        int po_id FK
        int product_id FK
        int quantity_ordered
        int quantity_received
        decimal unit_cost
    }
    STOCK_MOVEMENT {
        int movement_id PK
        int product_id FK
        int warehouse_id FK
        int po_id FK
        string movement_type
        int quantity
        string reason
        int created_at
    }
```

Notes:
- `STOCK_ITEM` has a unique constraint on (product_id, warehouse_id).
- `STOCK_MOVEMENT.movement_type` ∈ {IN, OUT, TRANSFER_OUT, TRANSFER_IN, ADJUSTMENT}.
- `PURCHASE_ORDER.status` ∈ {DRAFT, SENT, PARTIALLY_RECEIVED, RECEIVED, CANCELLED} — driven by the State pattern.
