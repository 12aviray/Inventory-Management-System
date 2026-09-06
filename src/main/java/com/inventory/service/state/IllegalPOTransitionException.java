package com.inventory.service.state;

public class IllegalPOTransitionException extends RuntimeException {
    public IllegalPOTransitionException(String message) {
        super(message);
    }
}
