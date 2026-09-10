package com.inventory.service.state;

public class ReceivedState implements POState {
    @Override public String name() { return "RECEIVED"; }
    @Override public boolean canSend() { return false; }
    @Override public boolean canReceive() { return false; }
    @Override public boolean canCancel() { return false; }

    @Override
    public POState send() {
        throw new IllegalPOTransitionException("Purchase order is already received");
    }

    @Override
    public POState receive(boolean isFullyReceived) {
        throw new IllegalPOTransitionException("Purchase order is already fully received");
    }

    @Override
    public POState cancel() {
        throw new IllegalPOTransitionException("Cannot cancel a fully received purchase order");
    }
}
