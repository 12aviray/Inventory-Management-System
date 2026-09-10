package com.inventory.service.state;

public class DraftState implements POState {
    @Override public String name() { return "DRAFT"; }
    @Override public boolean canSend() { return true; }
    @Override public boolean canReceive() { return false; }
    @Override public boolean canCancel() { return true; }

    @Override
    public POState send() {
        return new SentState();
    }

    @Override
    public POState receive(boolean isFullyReceived) {
        throw new IllegalPOTransitionException("Cannot receive a purchase order that has not been sent");
    }

    @Override
    public POState cancel() {
        return new CancelledState();
    }
}
