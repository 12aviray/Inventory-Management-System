package com.inventory.service;

import com.inventory.service.state.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class POStateTest {

    @Test
    void draft_canTransitionToSent() {
        POState draft = new DraftState();
        POState next = draft.send();
        assertEquals("SENT", next.name());
    }

    @Test
    void draft_cannotBeReceived() {
        POState draft = new DraftState();
        assertThrows(IllegalPOTransitionException.class, () -> draft.receive(true));
    }

    @Test
    void sent_fullyReceived_transitionsToReceived() {
        POState sent = new SentState();
        POState next = sent.receive(true);
        assertEquals("RECEIVED", next.name());
    }

    @Test
    void sent_partiallyReceived_transitionsToPartiallyReceived() {
        POState sent = new SentState();
        POState next = sent.receive(false);
        assertEquals("PARTIALLY_RECEIVED", next.name());
    }

    @Test
    void partiallyReceived_cannotBeCancelled() {
        POState partial = new PartiallyReceivedState();
        assertFalse(partial.canCancel());
        assertThrows(IllegalPOTransitionException.class, partial::cancel);
    }

    @Test
    void received_isTerminal() {
        POState received = new ReceivedState();
        assertFalse(received.canSend());
        assertFalse(received.canReceive());
        assertFalse(received.canCancel());
    }

    @Test
    void factory_mapsStatusStringsToCorrectStateType() {
        assertInstanceOf(DraftState.class, POStateFactory.fromStatus("DRAFT"));
        assertInstanceOf(SentState.class, POStateFactory.fromStatus("SENT"));
        assertInstanceOf(CancelledState.class, POStateFactory.fromStatus("CANCELLED"));
    }
}
