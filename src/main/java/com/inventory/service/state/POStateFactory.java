package com.inventory.service.state;

/** Maps the persisted status string to its corresponding State object and back. */
public final class POStateFactory {

    private POStateFactory() {
    }

    public static POState fromStatus(String status) {
        return switch (status) {
            case "DRAFT" -> new DraftState();
            case "SENT" -> new SentState();
            case "PARTIALLY_RECEIVED" -> new PartiallyReceivedState();
            case "RECEIVED" -> new ReceivedState();
            case "CANCELLED" -> new CancelledState();
            default -> throw new IllegalArgumentException("Unknown purchase order status: " + status);
        };
    }
}
