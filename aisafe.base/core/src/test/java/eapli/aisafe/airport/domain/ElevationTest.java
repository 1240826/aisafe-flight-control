package eapli.aisafe.airport.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Elevation value object.
 * US052.6: elevation must have a positive value and a non-blank unit.
 */
class ElevationTest {

    // ── Valid construction ─────────────────────────────────────────────────────

    @Test
    void ensurePositiveValueWithMetresUnitIsAccepted() {
        final var elev = new Elevation(69.0, "m");
        assertEquals(69.0, elev.value(), 0.001);
        assertEquals("m", elev.unit());
    }

    @Test
    void ensureLargeElevationIsAccepted() {
        // Lhasa Gonggar Airport — 3569 m
        final var elev = new Elevation(3569.0, "m");
        assertEquals(3569.0, elev.value(), 0.001);
    }

    @Test
    void ensureSmallPositiveElevationIsAccepted() {
        // 1 m is the minimum accepted
        final var elev = new Elevation(1.0, "m");
        assertEquals(1.0, elev.value(), 0.001);
    }

    @Test
    void ensureFeetUnitIsAccepted() {
        final var elev = new Elevation(226.0, "ft");
        assertEquals("ft", elev.unit());
    }

    // ── Invariant violations ───────────────────────────────────────────────────

    @Test
    void ensureNegativeValueIsRejected() {
        assertThrows(Exception.class, () -> new Elevation(-10.0, "m"),
                "Negative elevation should be rejected (US052.6 acceptance test)");
    }

    @Test
    void ensureZeroValueIsRejected() {
        assertThrows(Exception.class, () -> new Elevation(0.0, "m"),
                "Zero elevation should be rejected — spec requires strictly positive");
    }

    @Test
    void ensureBlankUnitIsRejected() {
        assertThrows(Exception.class, () -> new Elevation(100.0, ""),
                "Blank unit should be rejected");
    }

    @Test
    void ensureWhitespaceOnlyUnitIsRejected() {
        assertThrows(Exception.class, () -> new Elevation(100.0, "   "),
                "Whitespace-only unit should be rejected");
    }

    @Test
    void ensureNullUnitIsRejected() {
        assertThrows(Exception.class, () -> new Elevation(100.0, null),
                "Null unit should be rejected");
    }

    // ── Equality ───────────────────────────────────────────────────────────────

    @Test
    void ensureEqualElevationsAreEqual() {
        final var e1 = new Elevation(69.0, "m");
        final var e2 = new Elevation(69.0, "m");
        assertEquals(e1, e2);
    }

    @Test
    void ensureDifferentValuesAreNotEqual() {
        final var e1 = new Elevation(69.0, "m");
        final var e2 = new Elevation(100.0, "m");
        assertNotEquals(e1, e2);
    }

    @Test
    void ensureDifferentUnitsAreNotEqual() {
        final var e1 = new Elevation(69.0, "m");
        final var e2 = new Elevation(69.0, "ft");
        assertNotEquals(e1, e2);
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Test
    void ensureToStringContainsBothValueAndUnit() {
        final var elev = new Elevation(114.0, "m");
        final String s = elev.toString();
        assertTrue(s.contains("114"), "toString should contain value");
        assertTrue(s.contains("m"), "toString should contain unit");
    }
}
