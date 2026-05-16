package eapli.aisafe.aircontrolarea.domain;

import eapli.aisafe.shared.domain.Coordinates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the BoundingBox value object.
 */
class BoundingBoxTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static BoundingBox lisboaFIR() {
        return new BoundingBox(37.0, 42.0, -10.0, -6.0);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureValidBoundingBoxCanBeCreated() {
        final var box = lisboaFIR();
        assertEquals(37.0, box.minLat(), 0.001);
        assertEquals(42.0, box.maxLat(), 0.001);
        assertEquals(-10.0, box.minLon(), 0.001);
        assertEquals(-6.0, box.maxLon(), 0.001);
    }

    // ── Invariant violations ──────────────────────────────────────────────────

    @Test
    void ensureMinLatGreaterThanMaxLatIsRejected() {
        assertThrows(Exception.class, () -> new BoundingBox(42.0, 37.0, -10.0, -6.0),
                "minLat > maxLat must be rejected");
    }

    @Test
    void ensureEqualMinMaxLatIsRejected() {
        assertThrows(Exception.class, () -> new BoundingBox(42.0, 42.0, -10.0, -6.0),
                "minLat == maxLat must be rejected (strict less-than)");
    }

    @Test
    void ensureMinLonGreaterThanMaxLonIsRejected() {
        assertThrows(Exception.class, () -> new BoundingBox(37.0, 42.0, -6.0, -10.0),
                "minLon > maxLon must be rejected");
    }

    @Test
    void ensureLatitudeOutsideGlobalBoundsIsRejected() {
        assertThrows(Exception.class, () -> new BoundingBox(-91.0, 42.0, -10.0, -6.0),
                "minLat < -90 must be rejected");
    }

    @Test
    void ensureLongitudeOutsideGlobalBoundsIsRejected() {
        assertThrows(Exception.class, () -> new BoundingBox(37.0, 42.0, -10.0, 200.0),
                "maxLon > 180 must be rejected");
    }

    // ── contains ─────────────────────────────────────────────────────────────

    @Test
    void ensureInteriorPointIsContained() {
        final var box = lisboaFIR();
        assertTrue(box.contains(new Coordinates(39.0, -8.0)),
                "Interior point should be inside");
    }

    @Test
    void ensureCornerPointIsContained() {
        final var box = lisboaFIR();
        assertTrue(box.contains(new Coordinates(37.0, -10.0)),
                "Corner point should be inside (inclusive boundary)");
    }

    @Test
    void ensurePointNorthOfBoxIsNotContained() {
        final var box = lisboaFIR();
        assertFalse(box.contains(new Coordinates(50.0, -8.0)),
                "Point north of maxLat should not be inside");
    }

    @Test
    void ensurePointEastOfBoxIsNotContained() {
        final var box = lisboaFIR();
        assertFalse(box.contains(new Coordinates(39.0, 0.0)),
                "Point east of maxLon should not be inside");
    }

    // ── overlaps ─────────────────────────────────────────────────────────────

    @Test
    void ensureOverlappingBoxIsDetected() {
        final var box = lisboaFIR();
        assertTrue(box.overlaps(new BoundingBox(40.0, 45.0, -9.0, -5.0)),
                "Overlapping box should be detected");
    }

    @Test
    void ensureDisjointBoxNorthIsNotOverlapping() {
        final var box = lisboaFIR();
        assertFalse(box.overlaps(new BoundingBox(50.0, 55.0, -9.0, -5.0)),
                "Box completely north should not overlap");
    }

    @Test
    void ensureDisjointBoxSouthIsNotOverlapping() {
        final var box = lisboaFIR();
        assertFalse(box.overlaps(new BoundingBox(30.0, 35.0, -9.0, -5.0)),
                "Box completely south should not overlap");
    }

    @Test
    void ensureDisjointBoxEastIsNotOverlapping() {
        final var box = lisboaFIR();
        assertFalse(box.overlaps(new BoundingBox(39.0, 41.0, 5.0, 10.0)),
                "Box completely east should not overlap");
    }

    @Test
    void ensureBoundingBoxesWithSameValuesAreEqual() {
        final var b1 = lisboaFIR();
        final var b2 = new BoundingBox(37.0, 42.0, -10.0, -6.0);
        assertEquals(b1, b2);
    }

    @Test
    void ensureBoundingBoxesWithDifferentValuesAreNotEqual() {
        final var b1 = lisboaFIR();
        final var b2 = new BoundingBox(38.0, 42.0, -10.0, -6.0);
        assertNotEquals(b1, b2);
    }
}
