# Multi-Warehouse Inventory & Restocking System

A JavaFX + Maven + SQLite desktop application built for the Design Patterns Lab
final project. See `docs/` for the scope document, ER diagram, and design
pattern justifications.

## Prerequisites
- JDK 17 or later
- Maven 3.8+
- (Internet access on first build, to download JavaFX/SQLite/JUnit dependencies)

## Running the application
```bash
mvn clean javafx:run
```
On first run, `DatabaseSeeder` automatically creates `inventory.db` in the
project root (via `schema.sql`) and loads sample products, warehouses,
suppliers, and stock so the app has usable data immediately.

## Running tests
```bash
mvn test
```
Tests cover the Strategy implementations, the PurchaseOrder state machine,
and the PurchaseOrderBuilder validation — the core business logic, independent
of the UI and database.

## Project structure
```
src/main/java/com/inventory/
  model/                 entities + PurchaseOrderBuilder
  dao/                   JDBC data-access layer (one class per table/entity)
  service/               business logic
    strategy/            Strategy pattern — reorder calculation
    state/                State pattern — purchase order status
    observer/             Observer pattern — low-stock alerts
    command/               Command pattern — stock adjustments
  ui/controller/         JavaFX controllers (one per screen)
  ui/viewmodel/          small UI-only row/view classes
  util/                  DatabaseManager, DatabaseSeeder
src/main/resources/
  fxml/                  screen layouts
  db/schema.sql          DDL
src/test/java/           JUnit 5 tests for business logic
docs/                    scope, ER diagram, design pattern justifications
```

## Git workflow for this project
1. Create a feature branch per task, e.g. `feature/product-crud`,
   `feature/restocking-workflow`, `feature/receive-shipment-ui`.
2. Commit in small, meaningful chunks — not one giant commit per feature.
3. Open a pull request into `main` (or a `develop` integration branch) when
   the feature is complete and tested; review before merging.
4. Both team members should have visible, regular commits across the
   project's lifetime, not concentrated in one member or one late burst.

Suggested initial branches to split the remaining work:
- `feature/dashboard-polish`
- `feature/reports-filtering` (wire up the date/product/warehouse filters
  already supported by `StockMovementDao.search(...)` to the Reports UI)
- `feature/validation-hardening` (input validation, error dialogs instead of
  plain labels)
- `feature/tests-integration` (DAO-level tests against a temporary SQLite file)

## What's implemented vs. what to extend
This scaffold implements the full architecture end-to-end for every screen
listed in `docs/01-scope.md`, with real (not stubbed) business logic for the
core workflows. Natural next steps for your team to build on:
- Add confirmation dialogs and nicer error handling in place of status labels
- Wire the Reports screen's movement-history filters (product/warehouse/date
  range) to actual UI controls — the service method already supports it
- Add DAO-level integration tests using a temporary SQLite database file
- Consider adding authentication/roles if you want to distinguish
  Clerk vs. Manager permissions (mentioned in the scope doc's target users)
