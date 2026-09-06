# Design Patterns — Problem, Justification, Alternatives, Future Benefit

This document summarizes the "why" for each pattern used, for the demonstration and
documentation requirements of the project. Full justification also lives as Javadoc
comments directly on each pattern's interface in the source code.

---

## 1. Strategy — Reorder Calculation
**Location:** `service/strategy/ReorderStrategy.java`, `FixedThresholdStrategy`, `AverageDemandStrategy`

- **Problem:** Deciding whether a stock item needs reordering can be done multiple
  ways (fixed threshold per product vs. demand-based coverage), and the restocking
  workflow (`RestockingService`) shouldn't be rewritten every time a new rule is added.
- **Why this pattern:** `RestockingService` depends only on the `ReorderStrategy`
  interface. Swapping algorithms is a one-line change (`setStrategy(...)`), and the
  UI could expose this as a dropdown.
- **Alternative considered:** A single method with an `if/else`/`switch` on an
  "algorithm type" flag. Rejected — every new rule would require editing that method,
  risking regressions in existing rules, and each rule couldn't be unit-tested in isolation.
- **Future benefit:** Adding a seasonal-demand or supplier-lead-time-aware strategy
  later is a new class implementing the interface. No change to `RestockingService`
  or the reporting screen that consumes it.

## 2. State — Purchase Order Status
**Location:** `service/state/POState.java` and its 5 implementations, `POStateFactory`

- **Problem:** A `PurchaseOrder` moves through `DRAFT → SENT → PARTIALLY_RECEIVED →
  RECEIVED`, with `CANCELLED` reachable only from certain statuses. Scattermarking
  this with string/enum comparisons across the service layer risks inconsistent or
  duplicated transition logic.
- **Why this pattern:** Each status is a class that knows exactly which transitions
  are legal from it (`canSend()`, `canReceive()`, `canCancel()`). `PurchaseOrderService`
  asks the current state object rather than re-deriving the rules each time.
- **Alternative considered:** An enum with a big `switch` in the service for every
  action. Rejected — adding a new status (e.g. `ON_HOLD`) would mean touching every
  switch statement that checks status, an easy place to introduce bugs.
- **Future benefit:** A new status is a new class implementing `POState`; existing
  state classes and the service logic that calls them are untouched.

## 3. Observer — Low-Stock Alerts
**Location:** `service/observer/StockAlertObserver.java`, `StockAlertPublisher`, `AuditLogObserver`

- **Problem:** When a low-stock condition is detected, multiple independent parts of
  the app may want to react (dashboard badge refresh, audit log entry, and later
  perhaps an email notifier) without `RestockingService` needing to know about any
  of them directly.
- **Why this pattern:** `RestockingService` publishes a "low stock" event to any
  subscribed observers. New observers register themselves; the publisher's code
  never changes.
- **Alternative considered:** Calling `dashboard.refresh()` and `log.record(...)`
  directly inside the detection method. Rejected — couples business logic to
  specific UI/infra components and requires editing that method for every new
  notification channel.
- **Future benefit:** An email/SMS notifier for low stock is a new `StockAlertObserver`
  implementation registered at startup — zero changes to `RestockingService`.

## 4. Command — Stock Adjustments
**Location:** `service/command/StockCommand.java`, `ReceiveStockCommand`, `TransferStockCommand`

- **Problem:** Every kind of stock change (receiving, transferring, future manual
  adjustments) must update `stock_item` and write a `stock_movement` audit row
  *together*, consistently. Hand-rolling this in every workflow risks the audit
  step being forgotten in one of them.
- **Why this pattern:** Each kind of change is a self-contained command object with
  one `execute(Connection)` method that performs both steps atomically. Workflows
  just construct and execute the right command.
- **Alternative considered:** One `adjustStock(...)` method with a movement-type
  parameter and internal branching. Rejected — as movement types with different
  side effects grow (transfers touch two warehouses; receipts touch a PO line too),
  that method becomes an unreadable, untestable monolith.
- **Future benefit:** A future "manual adjustment with approval" workflow is a new
  `StockCommand` implementation, independently unit-testable, without touching
  `ReceiveStockCommand` or `TransferStockCommand`.

## 5. Builder — Purchase Order Construction
**Location:** `model/PurchaseOrderBuilder.java`

- **Problem:** A `PurchaseOrder` has one required field (supplier) plus optional
  ones (expected date, notes) and a variable number of line items added
  incrementally as the user fills out the "create PO" screen row by row.
- **Why this pattern:** The UI controller can call `.addLine(...)` repeatedly as
  rows are added, and `.build()` validates the whole thing (supplier present, at
  least one line) before construction completes — impossible to end up with a
  half-formed, invalid PO.
- **Alternative considered:** A plain setter-based POJO, as used for simpler
  entities like `Product`. Rejected specifically for `PurchaseOrder` because its
  construction is itself a multi-step process with end-of-construction validation
  — which is exactly what Builder solves and what `Product` doesn't need.
- **Future benefit:** Adding a new optional field (e.g. a discount code) to PO
  creation is one new builder method; existing call sites are unaffected.

## Patterns deliberately NOT used
- **Singleton** was considered for `DatabaseManager` but rejected as a *named
  pattern claim* — it's a static utility for connection config, not a case where
  ensuring "exactly one instance" solves a business problem. Forcing the GoF
  Singleton label onto it would be exactly the kind of pattern-for-pattern's-sake
  the project brief warns against.
- **Factory Method** for `Product`/`Warehouse`/`Supplier` was considered but
  rejected — these are simple CRUD entities with no varying construction logic,
  so a factory would add indirection without benefit. `PurchaseOrder` gets a
  Builder specifically because it *does* have that complexity; the others don't.
