package com.artiv.guide.session;

import com.artiv.common.BusinessException;
import com.artiv.config.GuideConfig;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class GuideSessionStoreTest {
    private final GuideConfig config = new GuideConfig();
    private final GuideSessionStore store = new GuideSessionStore(config);
    private GuideSessionStore.Run begin(String id, String text) { return store.begin(1, id, UUID.randomUUID().toString(), text, "trusted system"); }

    @Test void ownerIsRequiredForReadSendCancelAndDelete() {
        var id = store.create(1).id();
        assertThrows(BusinessException.class, () -> store.get(2, id));
        assertThrows(BusinessException.class, () -> store.begin(2, id, UUID.randomUUID().toString(), "x", "s"));
        assertThrows(BusinessException.class, () -> store.cancel(2, id, "x"));
        assertThrows(BusinessException.class, () -> store.delete(2, id));
        assertEquals(0, store.get(1, id).messages().size());
    }
    @Test void duplicateIdsAndConcurrentRunsDoNotCreateAnotherTurn() {
        var id = store.create(1).id();
        var run = begin(id, "first");
        assertThrows(BusinessException.class, () -> begin(id, "second"));
        var other = store.create(1).id();
        assertThrows(BusinessException.class, () -> begin(other, "second"));
        store.append(run, "answer"); store.finish(run, "complete", null, null);
        assertThrows(BusinessException.class, () -> store.begin(1, id, run.clientMessageId(), "changed", "system"));
        assertEquals(2, store.get(1, id).messages().size());
    }
    @Test void contextIsServerOwnedAndIncludesOnlyCompletedPairs() {
        var id = store.create(1).id();
        var first = begin(id, "remember 731");
        store.append(first, "remembered"); store.finish(first, "complete", null, null);
        var failed = begin(id, "ignored");
        store.append(failed, "partial"); store.finish(failed, "cancelled", null, null);
        var next = begin(id, "what number?");
        assertEquals(java.util.List.of("trusted system", "remember 731", "remembered", "what number?"), next.context().stream().map(m -> m.text()).toList());
    }
    @Test void cancellationAndClearingPreventLateDeltas() {
        var id = store.create(1).id();
        var run = begin(id, "hi");
        store.cancel(1, id, run.clientMessageId());
        assertTrue(run.cancellation().cancelled());
        assertFalse(store.append(run, "late"));
        store.finish(run, "cancelled", null, null);
        var next = begin(id, "again");
        store.delete(1, id);
        assertTrue(next.cancellation().cancelled());
        assertFalse(store.finish(next, "complete", null, null));
        assertThrows(BusinessException.class, () -> store.get(1, id));
    }
    @Test void boundedSessionsTurnsAndInput() {
        config.setMaxSessionsPerUser(1); config.setMaxTurns(1);
        var id = store.create(1).id();
        assertThrows(BusinessException.class, () -> store.create(1));
        assertThrows(BusinessException.class, () -> begin(id, " "));
        assertThrows(BusinessException.class, () -> begin(id, "a".repeat(4001)));
        var run = begin(id, "valid"); store.finish(run, "complete", null, null);
        assertThrows(BusinessException.class, () -> begin(id, "another"));
    }
    @Test void ttlEvictsAndCancelsExpiredRuns() {
        config.setSessionTtlMinutes(1);
        class MutableClock extends Clock {
            Instant now = Instant.parse("2026-09-06T00:00:00Z");
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return now; }
        }
        var clock = new MutableClock();
        var sessions = new GuideSessionStore(config, clock);
        var id = sessions.create(1).id();
        var run = sessions.begin(1, id, UUID.randomUUID().toString(), "x", "s");
        clock.now = clock.now.plusSeconds(61);
        assertThrows(BusinessException.class, () -> sessions.get(1, id));
        sessions.cleanup();
        assertTrue(run.cancellation().cancelled());
        assertThrows(BusinessException.class, () -> sessions.get(1, id));
    }
}
