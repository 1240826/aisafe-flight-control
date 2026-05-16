package eapli.aisafe.shared.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the shared Coordinates value object.
 */
class CoordinatesTest {

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureValidCoordinatesCanBeCreated() {
        final var c = new Coordinates(41.2481, -8.6814);
        assertEquals(41.2481, c.latitude(), 0.00001);
        assertEquals(-8.6814, c.longitude(), 0.00001);
    }

    @Test
    void ensureZeroLatLonIsValid() {
        final var c = new Coordinates(0.0, 0.0);
        assertEquals(0.0, c.latitude(), 0.0);
        assertEquals(0.0, c.longitude(), 0.0);
    }

    @Test
    void ensureMaxBoundaryValuesAreAccepted() {
        final var c = new Coordinates(90.0, 180.0);
        assertEquals(90.0, c.latitude(), 0.0);
        assertEquals(180.0, c.longitude(), 0.0);
    }

    @Test
    void ensureMinBoundaryValuesAreAccepted() {
        final var c = new Coordinates(-90.0, -180.0);
        assertEquals(-90.0, c.latitude(), 0.0);
        assertEquals(-180.0, c.longitude(), 0.0);
    }

    // ── Latitude invariants ───────────────────────────────────────────────────

    @Test
    void ensureLatitudeAbove90IsRejected() {
        assertThrows(Exception.class, () -> new Coordinates(90.001, 0.0),
                "Latitude > 90 must be rejected");
    }

    @Test
    void ensureLatitudeBelow90IsRejected() {
        assertThrows(Exception.class, () -> new Coordinates(-90.001, 0.0),
                "Latitude < -90 must be rejected");
    }

    // ── Longitude invariants ──────────────────────────────────────────────────

    @Test
    void ensureLongitudeAbove180IsRejected() {
        assertThrows(Exception.class, () -> new Coordinates(0.0, 180.001),
                "Longitude > 180 must be rejected");
    }

    @Test
    void ensureLongitudeBelow180IsRejected() {
        assertThrows(Exception.class, () -> new Coordinates(0.0, -180.001),
                "Longitude < -180 must be rejected");
    }

    // ── Equality ──────────────────────────────────────────────────────────────

    @Test
    void ensureCoordinatesWithSameValuesAreEqual() {
        final var c1 = new Coordinates(51.5074, -0.1278);
        final var c2 = new Coordinates(51.5074, -0.1278);
        assertEquals(c1, c2);
    }

    @Test
    void ensureCoordinatesWithDifferentLatAreNotEqual() {
        final var c1 = new Coordinates(51.5074, -0.1278);
        final var c2 = new Coordinates(48.8566, -0.1278);
        assertNotEquals(c1, c2);
    }

    @Test
    void ensureCoordinatesWithDifferentLonAreNotEqual() {
        final var c1 = new Coordinates(51.5074, -0.1278);
        final var c2 = new Coordinates(51.5074, 2.3522);
        assertNotEquals(c1, c2);
    }

    @Test
    void ensureToStringContainsLatAndLon() {
        final var c = new Coordinates(41.0, -8.0);
        final String s = c.toString();
        assertTrue(s.contains("41.0"), "toString should contain latitude");
        assertTrue(s.contains("-8.0"), "toString should contain longitude");
    }
}
