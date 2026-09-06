package com.inventory.service.state;

public class SentState implements POState {
    @Override public String name() { return "SENT"; }
    @Override public boolean canSend() { return false; }
    @Override public boolean canReceive() { return true; }
    @Override public boolean canCancel() { return true; }

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
        return new CancelledState();
    }
}
