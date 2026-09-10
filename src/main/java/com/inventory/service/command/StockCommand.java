package com.inventory.service.command;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Command pattern.
 *
 * Problem: stock can change for several distinct reasons (receiving a
 * shipment, transferring between warehouses, manual adjustment). Every one
 * of these needs to (a) update stock_item quantities and (b) write a
 * stock_movement audit row, atomically and consistently. If each workflow
 * hand-rolled this itself, the audit-logging step could be forgotten or
 * done inconsistently in one of the flows.
 *
 * Why Command: each kind of stock change is encapsulated as an object with
 * a single execute() method that performs both the quantity update and the
 * movement log in one place, given a shared Connection. Workflows create
 * and execute the appropriate command rather than duplicating the
 * update+log logic. It also opens the door to queuing/logging commands
 * uniformly, or supporting undo in the future.
 *
 * Alternative considered: a single "adjustStock(...)" method with a
 * movement-type parameter and internal if/else. Rejected because as more
 * movement types with different side effects appear (e.g. transfers touch
 * two warehouses), that method grows complex; separate command classes keep
 * each kind of change self-contained and independently testable.
 */
public interface StockCommand {
    void execute(Connection conn) throws SQLException;
}
