package com.inventory.service;

import com.inventory.dao.PurchaseOrderDao;
import com.inventory.model.PurchaseOrder;
import com.inventory.model.PurchaseOrderLine;
import com.inventory.service.command.ReceiveStockCommand;
import com.inventory.service.command.StockCommand;
import com.inventory.service.state.POState;
import com.inventory.service.state.POStateFactory;
import com.inventory.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Business logic for purchase orders: creation, sending, and receiving
 * shipments (full or partial). Delegates legality of status transitions to
 * the State pattern (POState) and stock/audit updates to Command objects.
 */
public class PurchaseOrderService {

    private final PurchaseOrderDao purchaseOrderDao = new PurchaseOrderDao();

    public PurchaseOrder create(PurchaseOrder po) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                purchaseOrderDao.insert(po, conn);
                conn.commit();
                return po;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create purchase order", e);
        }
    }

    public void send(int poId) {
        transitionStatus(poId, POState::send);
    }

    public void cancel(int poId) {
        transitionStatus(poId, POState::cancel);
    }

    /**
     * Receives a shipment against a PO. Each line's received-quantity is
     * updated, stock is increased via ReceiveStockCommand (Command pattern),
     * and the PO's overall status transitions via the State pattern
     * depending on whether every line is now fully received.
     */
    public void receiveShipment(int poId, int warehouseId, java.util.Map<Integer, Integer> receivedQuantitiesByLineId) {
        PurchaseOrder po = purchaseOrderDao.findById(poId)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found: " + poId));

        POState currentState = POStateFactory.fromStatus(po.getStatus());
        if (!currentState.canReceive()) {
            throw new IllegalStateException("Cannot receive shipment for PO in status " + po.getStatus());
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean allLinesFullyReceived = true;
                for (PurchaseOrderLine line : po.getLines()) {
                    Integer newlyReceived = receivedQuantitiesByLineId.get(line.getPoLineId());
                    if (newlyReceived != null && newlyReceived > 0) {
                        int updatedReceived = line.getQuantityReceived() + newlyReceived;
                        purchaseOrderDao.updateLineReceivedQuantity(line.getPoLineId(), updatedReceived, conn);

                        StockCommand command = new ReceiveStockCommand(
                                line.getProductId(), warehouseId, poId, newlyReceived);
                        command.execute(conn);

                        if (updatedReceived < line.getQuantityOrdered()) {
                            allLinesFullyReceived = false;
                        }
                    } else if (line.getOutstandingQuantity() > 0) {
                        allLinesFullyReceived = false;
                    }
                }

                POState nextState = currentState.receive(allLinesFullyReceived);
                purchaseOrderDao.updateStatus(poId, nextState.name(), conn);

                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to receive shipment for PO " + poId, e);
        }
    }

    public List<PurchaseOrder> findAll() {
        return purchaseOrderDao.findAll();
    }

    private interface StateTransition {
        POState apply(POState state);
    }

    private void transitionStatus(int poId, StateTransition transition) {
        PurchaseOrder po = purchaseOrderDao.findById(poId)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found: " + poId));
        POState currentState = POStateFactory.fromStatus(po.getStatus());
        POState nextState = transition.apply(currentState);

        try (Connection conn = DatabaseManager.getConnection()) {
            purchaseOrderDao.updateStatus(poId, nextState.name(), conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update PO status", e);
        }
    }
}
