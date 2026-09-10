# UML Class Diagrams

These diagrams cover the class structures behind the major design decisions
discussed in `03-design-patterns.md`. The plain CRUD entities (`Product`,
`Warehouse`, `Supplier`) are omitted since they have no interesting
structure — see `02-er-diagram.md` for their data shape instead.

## Strategy — Reorder Calculation

```mermaid
classDiagram
    class ReorderStrategy {
        <<interface>>
        +needsReorder(StockItem, Product) boolean
        +suggestedQuantity(StockItem, Product) int
    }
    class FixedThresholdStrategy {
        +needsReorder(StockItem, Product) boolean
        +suggestedQuantity(StockItem, Product) int
    }
    class AverageDemandStrategy {
        +needsReorder(StockItem, Product) boolean
        +suggestedQuantity(StockItem, Product) int
    }
    class RestockingService {
        -ReorderStrategy strategy
        +setStrategy(ReorderStrategy) void
        +findLowStockItems() List~LowStockRow~
    }
    ReorderStrategy <|.. FixedThresholdStrategy
    ReorderStrategy <|.. AverageDemandStrategy
    RestockingService o-- ReorderStrategy
```

## State — Purchase Order Status

```mermaid
classDiagram
    class POState {
        <<interface>>
        +canSend() boolean
        +canReceive() boolean
        +canCancel() boolean
        +status() String
    }
    class DraftState
    class SentState
    class PartiallyReceivedState
    class ReceivedState
    class CancelledState
    class POStateFactory {
        +forStatus(String) POState
    }
    class PurchaseOrderService {
        +send(int poId) void
        +receiveLine(...) void
        +cancel(int poId) void
    }
    POState <|.. DraftState
    POState <|.. SentState
    POState <|.. PartiallyReceivedState
    POState <|.. ReceivedState
    POState <|.. CancelledState
    POStateFactory ..> POState : creates
    PurchaseOrderService ..> POStateFactory
    PurchaseOrderService ..> POState
```

## Observer — Low-Stock Alerts

```mermaid
classDiagram
    class StockAlertObserver {
        <<interface>>
        +onLowStock(LowStockRow) void
    }
    class StockAlertPublisher {
        -List~StockAlertObserver~ observers
        +subscribe(StockAlertObserver) void
        +publish(LowStockRow) void
    }
    class AuditLogObserver {
        +onLowStock(LowStockRow) void
    }
    class RestockingService {
        +checkAndPublish() void
    }
    StockAlertObserver <|.. AuditLogObserver
    StockAlertPublisher o-- StockAlertObserver
    RestockingService ..> StockAlertPublisher
```

## Command — Stock Adjustments

```mermaid
classDiagram
    class StockCommand {
        <<interface>>
        +execute(Connection) void
    }
    class ReceiveStockCommand {
        -int poLineId
        -int quantity
        +execute(Connection) void
    }
    class SellStockCommand {
        -int productId
        -int warehouseId
        -int quantity
        +execute(Connection) void
    }
    class TransferStockCommand {
        -int productId
        -int fromWarehouseId
        -int toWarehouseId
        -int quantity
        +execute(Connection) void
    }
    class TransferService {
        +transfer(...) void
    }
    class SalesService {
        +processSale(...) void
    }
    StockCommand <|.. ReceiveStockCommand
    StockCommand <|.. SellStockCommand
    StockCommand <|.. TransferStockCommand
    TransferService ..> TransferStockCommand : constructs & executes
    SalesService ..> SellStockCommand : constructs & executes
```

## Builder — Purchase Order Construction

```mermaid
classDiagram
    class PurchaseOrder {
        -int poId
        -int supplierId
        -String status
        -List~PurchaseOrderLine~ lines
    }
    class PurchaseOrderBuilder {
        -PurchaseOrder order
        +supplier(int) PurchaseOrderBuilder
        +expectedDate(long) PurchaseOrderBuilder
        +notes(String) PurchaseOrderBuilder
        +addLine(int, int, double) PurchaseOrderBuilder
        +build() PurchaseOrder
    }
    PurchaseOrderBuilder ..> PurchaseOrder : builds
```

## DAO / Repository — Persistence Layer

```mermaid
classDiagram
    class ProductDao {
        +findAll() List~Product~
        +findById(int) Optional~Product~
        +insert(Product) Product
        +update(Product) void
        +delete(int) void
    }
    class WarehouseDao {
        +findAll() List~Warehouse~
        +findById(int) Optional~Warehouse~
        +insert(Warehouse) Warehouse
        +update(Warehouse) void
        +delete(int) void
    }
    class SupplierDao {
        +findAll() List~Supplier~
        +findById(int) Optional~Supplier~
        +insert(Supplier) Supplier
        +update(Supplier) void
        +delete(int) void
    }
    class ProductService
    class WarehouseService
    class SupplierService
    ProductService o-- ProductDao
    WarehouseService o-- WarehouseDao
    SupplierService o-- SupplierDao
```

Note: DAOs don't share a common generic interface (no `Dao<T>` superclass).
That was a deliberate choice, not an oversight — each entity's persistence
needs differ enough (e.g. `PurchaseOrderDao` needs an externally-supplied
`Connection` for transactional multi-table writes; `ProductDao` doesn't)
that a forced common interface would need awkward unused methods or
overly generic signatures, which is exactly the kind of pattern-for-its-own-sake
the project brief warns against.
