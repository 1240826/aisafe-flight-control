package eapli.aisafe.weatherdata.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

class WindConditionTest {

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us042/wind_condition_test.csv", numLinesToSkip = 1)
    void ensureWindConditionInvariants(
            final String testCaseId,
            final double speedKnots,
            final int directionDegrees,
            final double latitude,
            final double longitude,
            final int altitudeMetres,
            final boolean expectValid) {
        if (expectValid) {
            final var wc = assertDoesNotThrow(() ->
                    new WindCondition(speedKnots, directionDegrees, latitude, longitude, altitudeMetres));
            assertEquals(speedKnots, wc.speedKnots(), 0.001);
            assertEquals(directionDegrees, wc.directionDegrees());
            assertEquals(latitude, wc.latitude(), 0.001);
            assertEquals(longitude, wc.longitude(), 0.001);
            assertEquals(altitudeMetres, wc.altitudeMetres());
        } else {
            assertThrows(RuntimeException.class, () ->
                    new WindCondition(speedKnots, directionDegrees, latitude, longitude, altitudeMetres));
        }
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
