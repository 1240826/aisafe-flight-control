package eapli.aisafe.flightplan.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanIdTest {

    @Test
    void ensureValueOfCreatesValidId() {
        final var id = FlightPlanId.valueOf("FP001");
        assertEquals("FP001", id.toString());
    }

    @Test
    void ensureValueOfTrimsAndUppercases() {
        final var id = FlightPlanId.valueOf("  fp002  ");
        assertEquals("FP002", id.toString());
    }

    @Test
    void ensureNullIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FlightPlanId(null));
    }

    @Test
    void ensureBlankIdIsRejected() {
        assertThrows(IllegalStateException.class, () -> new FlightPlanId("   "));
    }

    @Test
    void ensureIdTooLongIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> new FlightPlanId("ABCDEFGHIJKLMNOPQRSTU"));
    }

    @Test
    void ensureIdWithSpecialCharsIsRejected() {
        assertThrows(IllegalStateException.class, () -> new FlightPlanId("FP@123"));
    }

    @Test
    void ensureEqualsAndHashCode() {
        final var id1 = FlightPlanId.valueOf("FP001");
        final var id2 = FlightPlanId.valueOf("FP001");
        final var id3 = FlightPlanId.valueOf("FP002");
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1, id3);
    }

    @Test
    void ensureCompareToWorks() {
        final var a = FlightPlanId.valueOf("A001");
        final var b = FlightPlanId.valueOf("B001");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }
}
