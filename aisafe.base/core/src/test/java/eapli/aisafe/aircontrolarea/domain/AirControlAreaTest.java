package eapli.aisafe.aircontrolarea.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AirControlArea aggregate root.
 * Covers US050 invariants: coordinate bounds, max altitude, overlap detection.
 */
class AirControlAreaTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static AirControlArea validArea() {
        return new AirControlArea(
                new AreaCode("LPPC"),
                new AreaName("Lisboa FIR"),
                37.0, 42.0,
                -10.0, -6.0,
                14000
        );
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureValidAreaCanBeCreated() {
        final var area = validArea();
        assertNotNull(area);
        assertEquals("LPPC", area.code().toString());
        assertEquals("Lisboa FIR", area.name().toString());
        assertEquals(14000, area.maxAltitudeMetres());
    }

    @Test
    void ensureIdentityReturnsAreaCode() {
        final var area = validArea();
        assertEquals(new AreaCode("LPPC"), area.identity());
    }

    @Test
    void ensureBoundaryCoordinatesArePreserved() {
        final var area = validArea();
        assertEquals(37.0, area.minLat(), 0.001);
        assertEquals(42.0, area.maxLat(), 0.001);
        assertEquals(-10.0, area.minLon(), 0.001);
        assertEquals(-6.0, area.maxLon(), 0.001);
    }

    // ── containsCoordinates ───────────────────────────────────────────────────

    @Test
    void ensureCoordinatesInsideBoundsAreContained() {
        final var area = validArea();
        assertTrue(area.containsCoordinates(39.0, -8.0),
                "Interior point should be inside");
    }

    @Test
    void ensureCoordinatesOnBoundaryAreContained() {
        final var area = validArea();
        assertTrue(area.containsCoordinates(37.0, -10.0),
                "Corner point should be inside (inclusive boundary)");
    }

    @Test
    void ensureCoordinatesOutsideLatitudeAreNotContained() {
        final var area = validArea();
        assertFalse(area.containsCoordinates(50.0, -8.0),
                "Point north of maxLat should not be inside");
    }

    @Test
    void ensureCoordinatesOutsideLongitudeAreNotContained() {
        final var area = validArea();
        assertFalse(area.containsCoordinates(39.0, 0.0),
                "Point east of maxLon should not be inside");
    }

    // ── overlapsWith ─────────────────────────────────────────────────────────

    @Test
    void ensureOverlappingRectangleIsDetected() {
        final var area = validArea();
        // Overlaps: [40-45] lat × [-9 to -5] lon
        assertTrue(area.overlapsWith(40.0, 45.0, -9.0, -5.0));
    }

    @Test
    void ensureDisjointRectangleNorthIsNotOverlapping() {
        final var area = validArea();
        // Completely north: [50-55] lat
        assertFalse(area.overlapsWith(50.0, 55.0, -9.0, -5.0));
    }

    @Test
    void ensureDisjointRectangleSouthIsNotOverlapping() {
        final var area = validArea();
        // Completely south: [30-35] lat
        assertFalse(area.overlapsWith(30.0, 35.0, -9.0, -5.0));
    }

    @Test
    void ensureDisjointRectangleEastIsNotOverlapping() {
        final var area = validArea();
        // Completely east of area: lon [5-10]
        assertFalse(area.overlapsWith(39.0, 41.0, 5.0, 10.0));
    }

    @Test
    void ensureRectangleWithGapBelowSouthBoundaryIsNotOverlapping() {
        final var area = validArea();
        // Area: lat [37, 42]; other: lat [30, 36.9] — clearly below south boundary
        assertFalse(area.overlapsWith(30.0, 36.9, -10.0, -6.0),
                "Rectangle clearly below south boundary should not overlap");
    }

    // ── Invariant violations ──────────────────────────────────────────────────

    @Test
    void ensureMinLatGreaterThanMaxLatIsRejected() {
        assertThrows(Exception.class, () -> new AirControlArea(
                new AreaCode("LPPC"),
                new AreaName("Lisboa FIR"),
                42.0, 37.0,  // minLat > maxLat — invalid
                -10.0, -6.0,
                14000));
    }

    @Test
    void ensureMinLonGreaterThanMaxLonIsRejected() {
        assertThrows(Exception.class, () -> new AirControlArea(
                new AreaCode("LPPC"),
                new AreaName("Lisboa FIR"),
                37.0, 42.0,
                -6.0, -10.0,  // minLon > maxLon — invalid
                14000));
    }

    @Test
    void ensureZeroMaxAltitudeIsRejected() {
        assertThrows(Exception.class, () -> new AirControlArea(
                new AreaCode("LPPC"),
                new AreaName("Lisboa FIR"),
                37.0, 42.0,
                -10.0, -6.0,
                0));
    }

    @Test
    void ensureNegativeMaxAltitudeIsRejected() {
        assertThrows(Exception.class, () -> new AirControlArea(
                new AreaCode("LPPC"),
                new AreaName("Lisboa FIR"),
                37.0, 42.0,
                -10.0, -6.0,
                -500));
    }

    @Test
    void ensureNullAreaCodeIsRejected() {
        assertThrows(Exception.class, () -> new AirControlArea(
                null,
                new AreaName("Lisboa FIR"),
                37.0, 42.0,
                -10.0, -6.0,
                14000));
    }

    @Test
    void ensureNullAreaNameIsRejected() {
        assertThrows(Exception.class, () -> new AirControlArea(
                new AreaCode("LPPC"),
                null,
                37.0, 42.0,
                -10.0, -6.0,
                14000));
    }
}
