package eapli.aisafe.weatherdata.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WindCondition value object.
 * Covers US041 invariants: speed, direction, latitude, longitude, altitude.
 */
class WindConditionTest {

    private static WindCondition valid() {
        return new WindCondition(15.5, 270, 38.7, -9.1, 1000);
    }

    @Test
    void ensureValidWindConditionCanBeCreated() {
        assertNotNull(valid());
    }

    @Test
    void ensureSpeedIsPreserved() {
        assertEquals(15.5, valid().speedKnots(), 0.001);
    }

    @Test
    void ensureDirectionIsPreserved() {
        assertEquals(270, valid().directionDegrees());
    }

    @Test
    void ensureLatitudeIsPreserved() {
        assertEquals(38.7, valid().latitude(), 0.001);
    }

    @Test
    void ensureLongitudeIsPreserved() {
        assertEquals(-9.1, valid().longitude(), 0.001);
    }

    @Test
    void ensureAltitudeIsPreserved() {
        assertEquals(1000, valid().altitudeMetres());
    }

    @Test
    void ensureZeroDirectionIsAccepted() {
        assertDoesNotThrow(() -> new WindCondition(10.0, 0, 0.0, 0.0, 0));
    }

    @Test
    void ensureDirection359IsAccepted() {
        assertDoesNotThrow(() -> new WindCondition(10.0, 359, 0.0, 0.0, 0));
    }

    @Test
    void ensureZeroAltitudeIsAccepted() {
        assertDoesNotThrow(() -> new WindCondition(10.0, 90, 0.0, 0.0, 0));
    }

    @Test
    void ensureMaxLatitudeIsAccepted() {
        assertDoesNotThrow(() -> new WindCondition(10.0, 90, 90.0, 0.0, 0));
    }

    @Test
    void ensureMinLatitudeIsAccepted() {
        assertDoesNotThrow(() -> new WindCondition(10.0, 90, -90.0, 0.0, 0));
    }

    @Test
    void ensureMaxLongitudeIsAccepted() {
        assertDoesNotThrow(() -> new WindCondition(10.0, 90, 0.0, 180.0, 0));
    }

    @Test
    void ensureMinLongitudeIsAccepted() {
        assertDoesNotThrow(() -> new WindCondition(10.0, 90, 0.0, -180.0, 0));
    }

    @Test
    void ensureZeroSpeedIsRejected() {
        assertThrows(Exception.class, () -> new WindCondition(0.0, 270, 38.7, -9.1, 1000));
    }

    @Test
    void ensureNegativeSpeedIsRejected() {
        assertThrows(Exception.class, () -> new WindCondition(-5.0, 270, 38.7, -9.1, 1000));
    }

    @Test
    void ensureDirection360IsRejected() {
        assertThrows(Exception.class, () -> new WindCondition(10.0, 360, 0.0, 0.0, 0));
    }

    @Test
    void ensureNegativeDirectionIsRejected() {
        assertThrows(Exception.class, () -> new WindCondition(10.0, -1, 0.0, 0.0, 0));
    }

    @Test
    void ensureLatitudeAbove90IsRejected() {
        assertThrows(Exception.class, () -> new WindCondition(10.0, 90, 91.0, 0.0, 0));
    }

    @Test
    void ensureLatitudeBelow90IsRejected() {
        assertThrows(Exception.class, () -> new WindCondition(10.0, 90, -91.0, 0.0, 0));
    }

    @Test
    void ensureLongitudeAbove180IsRejected() {
        assertThrows(Exception.class, () -> new WindCondition(10.0, 90, 0.0, 181.0, 0));
    }

    @Test
    void ensureLongitudeBelow180IsRejected() {
        assertThrows(Exception.class, () -> new WindCondition(10.0, 90, 0.0, -181.0, 0));
    }

    @Test
    void ensureNegativeAltitudeIsRejected() {
        assertThrows(Exception.class, () -> new WindCondition(10.0, 90, 0.0, 0.0, -1));
    }

    @Test
    void ensureTwoEqualWindConditionsAreEqual() {
        final var w1 = new WindCondition(15.5, 270, 38.7, -9.1, 1000);
        final var w2 = new WindCondition(15.5, 270, 38.7, -9.1, 1000);
        assertEquals(w1, w2);
    }

    @Test
    void ensureDifferentSpeedsAreNotEqual() {
        assertNotEquals(
                new WindCondition(15.5, 270, 38.7, -9.1, 1000),
                new WindCondition(20.0, 270, 38.7, -9.1, 1000));
    }

    @Test
    void ensureDifferentDirectionsAreNotEqual() {
        assertNotEquals(
                new WindCondition(15.5, 270, 38.7, -9.1, 1000),
                new WindCondition(15.5, 180, 38.7, -9.1, 1000));
    }

    @Test
    void ensureToStringContainsSpeed() {
        final var w = new WindCondition(15.5, 270, 38.7, -9.1, 1000);
        assertNotNull(w.toString());
        assertFalse(w.toString().isBlank());
    }

    @Test
    void ensureHashCodeIsConsistent() {
        final var w1 = new WindCondition(15.5, 270, 38.7, -9.1, 1000);
        final var w2 = new WindCondition(15.5, 270, 38.7, -9.1, 1000);
        assertEquals(w1.hashCode(), w2.hashCode());
    }
}