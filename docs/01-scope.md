# Project Scope — Multi-Warehouse Inventory & Restocking System

## Problem
Small-to-medium retail/distribution businesses that operate multiple warehouses struggle to
track stock accurately, know when to reorder, and move inventory between locations without
manual spreadsheets. This application manages products, suppliers, multi-warehouse stock
levels, purchase ordering, and inter-warehouse transfers, with reporting for decision-making.

## Target Users
- **Inventory Clerk** — records stock movements, receives shipments, performs transfers.
- **Manager** — reviews low-stock alerts, approves purchase orders, views reports.

## Core Entities (6)
1. Product
2. Warehouse
3. StockItem (Product × Warehouse quantity)
4. Supplier
5. PurchaseOrder / PurchaseOrderLine
6. StockMovement (audit log)

## Meaningful Workflows (3)
1. **Restocking workflow** — low-stock detection → suggested PO → review/edit → send →
   receive shipment → stock updated → movement logged. PO moves through states
   (DRAFT → SENT → PARTIALLY_RECEIVED → RECEIVED → CANCELLED).
2. **Stock transfer workflow** — move stock between warehouses with validation
   (source ≠ destination, positive quantity, sufficient source stock). Implemented
   as a single atomic `TransferStockCommand` (decrement source, increment
   destination, log two `stock_movement` rows) rather than a multi-status state
   machine — a transfer here is instantaneous, so there's no intermediate status
   worth modeling as a State pattern the way `PurchaseOrder` needs one.
3. **Reorder calculation** — pluggable strategies (fixed threshold vs. average-demand-based)
   used by the low-stock detector.
4. **Sales workflow** — dispatch stock to a customer via `SellStockCommand`
   (validates sufficient stock, decrements it, logs an `OUT` movement with
   sale price), feeding the profitability report.

## Reporting / Analytical Features (2)
1. Low-stock report across all warehouses (per selected strategy).
2. Stock movement / audit history report, filterable by date range, warehouse, product.

## Screens (8)
1. Dashboard (low-stock alerts, quick stats)
2. Product management (CRUD)
3. Warehouse stock view
4. Purchase order creation/review
5. Receive shipment screen
6. Sales / sell items screen
7. Stock transfer screen
8. Reports screen
9. Settings (Warehouse and Supplier CRUD, tabbed)

## CRUD Entities (3+)
Full create/read/update/delete is implemented for the three entities where
editing an existing record after creation is a real, expected operation:
**Product**, **Warehouse**, and **Supplier**. `PurchaseOrder` intentionally
gets create/read/update-status but no delete — cancelling (a State-pattern
transition) is the correct domain equivalent of "removing" a PO, since a
real purchase order is a business record that shouldn't disappear.
`StockItem` and `StockMovement` are only ever mutated through the Command
pattern (`StockCommand` implementations), by design, so that every stock
change stays auditable rather than being a raw, untracked edit.

## Design Patterns Applied (and why — see docs/03-design-patterns.md for full justification)
- **Strategy** — interchangeable reorder-calculation algorithms.
- **State** — PurchaseOrder and Transfer status transitions with legal-move validation.
- **Observer** — low-stock alerting; UI components subscribe to stock-change events.
- **Command** — stock adjustments encapsulated for consistent logging/auditing.
- **DAO / Repository** — persistence layer decoupled from business logic.
- **Builder** — PurchaseOrder construction with several optional fields.
