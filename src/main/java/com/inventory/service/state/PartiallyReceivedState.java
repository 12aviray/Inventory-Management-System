package com.inventory.service.state;

public class PartiallyReceivedState implements POState {
    @Override public String name() { return "PARTIALLY_RECEIVED"; }
    @Override public boolean canSend() { return false; }
    @Override public boolean canReceive() { return true; }
    @Override public boolean canCancel() { return false; } // already partly fulfilled by supplier

    @Override
    public POState send() {
        throw new IllegalPOTransitionException("Purchase order has already been sent");
    }

    @Override
    public POState receive(boolean isFullyReceived) {
        return isFullyReceived ? new ReceivedState() : new PartiallyReceivedState();
    }

    @Override
    public POState cancel() {
        throw new IllegalPOTransitionException("Cannot cancel a purchase order that is already partially received");
    }
}
