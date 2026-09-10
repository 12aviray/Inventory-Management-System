package com.inventory.service.state;

public class CancelledState implements POState {
    @Override public String name() { return "CANCELLED"; }
    @Override public boolean canSend() { return false; }
    @Override public boolean canReceive() { return false; }
    @Override public boolean canCancel() { return false; }

    @Override
    public POState send() {
        throw new IllegalPOTransitionException("Cannot send a cancelled purchase order");
    }

    @Override
    public POState receive(boolean isFullyReceived) {
        throw new IllegalPOTransitionException("Cannot receive a cancelled purchase order");
    }

    @Override
    public POState cancel() {
        throw new IllegalPOTransitionException("Purchase order is already cancelled");
    }
}
