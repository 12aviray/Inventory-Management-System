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
   (source availability, transfer states: REQUESTED → IN_TRANSIT → COMPLETED).
3. **Reorder calculation** — pluggable strategies (fixed threshold vs. average-demand-based)
   used by the low-stock detector.

## Reporting / Analytical Features (2)
1. Low-stock report across all warehouses (per selected strategy).
2. Stock movement / audit history report, filterable by date range, warehouse, product.

## Screens (7)
1. Dashboard (low-stock alerts, quick stats)
2. Product management (CRUD)
3. Warehouse stock view
4. Purchase order creation/review
5. Receive shipment screen
6. Stock transfer screen
7. Reports screen

## Design Patterns Applied (and why — see docs/03-design-patterns.md for full justification)
- **Strategy** — interchangeable reorder-calculation algorithms.
- **State** — PurchaseOrder and Transfer status transitions with legal-move validation.
- **Observer** — low-stock alerting; UI components subscribe to stock-change events.
- **Command** — stock adjustments encapsulated for consistent logging/auditing.
- **DAO / Repository** — persistence layer decoupled from business logic.
- **Builder** — PurchaseOrder construction with several optional fields.
