package com.inventory.service.state;

/**
 * State pattern.
 *
 * Problem: PurchaseOrder moves through statuses (DRAFT -> SENT ->
 * PARTIALLY_RECEIVED -> RECEIVED, with CANCELLED reachable from DRAFT/SENT)
 * and not every transition is legal from every status - e.g. you cannot
 * "receive" a DRAFT order, and you cannot cancel a RECEIVED order. Without
 * this pattern, that logic tends to end up as a large if/else or switch
 * block scattered across the service layer, repeated everywhere the status
 * is checked, and easy to get inconsistent.
 *
 * Why State: each status becomes a class that knows exactly which
 * transitions are legal from it. PurchaseOrderService just asks the current
 * state object "can I send/receive/cancel from here?" instead of
 * duplicating status-comparison logic.
 *
 * Alternative considered: an enum with a switch statement in the service
 * for each action. Rejected because as workflows grow (e.g. adding a
 * "PARTIALLY_CANCELLED" or "ON_HOLD" status later), every switch statement
 * touching status would need editing; here, only a new state class is added.
 *
 * Future benefit: adding a new status (e.g. ON_HOLD) means writing one new
 * class implementing this interface; no existing state classes change.
 */
public interface POState {
    String name();

    boolean canSend();
    boolean canReceive();
    boolean canCancel();

    /** Returns the state to transition to after successfully sending. */
    POState send();

    /** Returns the state after receiving (fully or partially). */
    POState receive(boolean isFullyReceived);

    POState cancel();
}
