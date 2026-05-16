package eapli.aisafe.weatherdata.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WeatherData aggregate root.
 * Covers US041 invariants: recordedDateTime, sourceProvider,
 * WindCondition coordinates, null params.
 */
class WeatherDataTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static final LocalDateTime RECORDED_AT = LocalDateTime.of(2026, 5, 14, 10, 0);

    private static WindCondition validWind() {
        return new WindCondition(15.5, 270, 38.7, -9.1, 1000);
    }

    private static WeatherData validWeatherData() {
        return new WeatherData(
                new AreaCode("LPPC"),
                validWind(),
                18.5,
                "IPMA",
                RECORDED_AT
        );
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureValidWeatherDataCanBeCreated() {
        final var wd = validWeatherData();
        assertNotNull(wd);
        assertEquals(18.5, wd.temperatureCelsius(), 0.001);
        assertEquals(RECORDED_AT, wd.recordedDateTime());
    }

    @Test
    void ensureAreaCodeIsPreserved() {
        final var wd = validWeatherData();
        assertEquals(new AreaCode("LPPC"), wd.areaCode());
    }

    @Test
    void ensureWindConditionIsPreserved() {
        final var wd = validWeatherData();
        assertEquals(15.5, wd.windCondition().speedKnots(), 0.001);
        assertEquals(270, wd.windCondition().directionDegrees());
        assertEquals(38.7, wd.windCondition().latitude(), 0.001);
        assertEquals(-9.1, wd.windCondition().longitude(), 0.001);
        assertEquals(1000, wd.windCondition().altitudeMetres());
    }

    @Test
    void ensureSourceProviderIsPreserved() {
        final var wd = validWeatherData();
        assertEquals("IPMA", wd.sourceProvider());
    }

    @Test
    void ensureNegativeTemperatureIsAccepted() {
        final var wd = new WeatherData(
                new AreaCode("BIRD"),
                new WindCondition(30.0, 180, 65.0, -18.0, 500),
                -35.0,
                "Isavia",
                RECORDED_AT);
        assertEquals(-35.0, wd.temperatureCelsius(), 0.001);
    }

    // ── Invariant violations — null params ────────────────────────────────────

    @Test
    void ensureNullAreaCodeIsRejected() {
        assertThrows(Exception.class, () -> new WeatherData(
                null, validWind(), 18.5, "IPMA", RECORDED_AT));
    }

    @Test
    void ensureNullWindConditionIsRejected() {
        assertThrows(Exception.class, () -> new WeatherData(
                new AreaCode("LPPC"), null, 18.5, "IPMA", RECORDED_AT));
    }

    @Test
    void ensureNullSourceProviderIsRejected() {
        assertThrows(Exception.class, () -> new WeatherData(
                new AreaCode("LPPC"), validWind(), 18.5, null, RECORDED_AT));
    }

    @Test
    void ensureBlankSourceProviderIsRejected() {
        assertThrows(Exception.class, () -> new WeatherData(
                new AreaCode("LPPC"), validWind(), 18.5, "   ", RECORDED_AT),
                "Blank source provider must be rejected");
    }

    @Test
    void ensureNullRecordedDateTimeIsRejected() {
        assertThrows(Exception.class, () -> new WeatherData(
                new AreaCode("LPPC"), validWind(), 18.5, "IPMA", null));
    }
}
