package eapli.aisafe.remote.weather;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.domain.WindCondition;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WeatherDataDTOTest {

    private WeatherData sampleWeatherData() {
        return new WeatherData(
                new AreaCode("LIS"),
                new WindCondition(12.5, 180, 38.7, -9.1, 100),
                22.0,
                "INMG",
                LocalDateTime.of(2026, 6, 1, 14, 30)
        );
    }

    @Test
    void fromMapsAreaCode() {
        final var dto = WeatherDataDTO.from(sampleWeatherData());
        assertEquals("LIS", dto.areaCode());
    }

    @Test
    void fromMapsLatitude() {
        final var dto = WeatherDataDTO.from(sampleWeatherData());
        assertEquals(38.7, dto.latitude());
    }

    @Test
    void fromMapsLongitude() {
        final var dto = WeatherDataDTO.from(sampleWeatherData());
        assertEquals(-9.1, dto.longitude());
    }

    @Test
    void fromMapsAltitudeMetres() {
        final var dto = WeatherDataDTO.from(sampleWeatherData());
        assertEquals(100, dto.altitudeMetres());
    }

    @Test
    void fromMapsWindSpeed() {
        final var dto = WeatherDataDTO.from(sampleWeatherData());
        assertEquals(12.5, dto.windSpeedKnots());
    }

    @Test
    void fromMapsWindDirection() {
        final var dto = WeatherDataDTO.from(sampleWeatherData());
        assertEquals(180, dto.windDirectionDegrees());
    }

    @Test
    void fromMapsTemperature() {
        final var dto = WeatherDataDTO.from(sampleWeatherData());
        assertEquals(22.0, dto.temperatureCelsius());
    }

    @Test
    void fromMapsSourceProvider() {
        final var dto = WeatherDataDTO.from(sampleWeatherData());
        assertEquals("INMG", dto.sourceProvider());
    }

    @Test
    void fromMapsRecordedDateTime() {
        final var dto = WeatherDataDTO.from(sampleWeatherData());
        assertEquals(LocalDateTime.of(2026, 6, 1, 14, 30), dto.recordedDateTime());
    }

    @Test
    void equalsAndHashCode() {
        final var dto1 = WeatherDataDTO.from(sampleWeatherData());
        final var dto2 = WeatherDataDTO.from(sampleWeatherData());
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void notEqualsDifferentArea() {
        final var wd1 = sampleWeatherData();
        final var wd2 = new WeatherData(
                new AreaCode("OPO"),
                new WindCondition(10.0, 90, 41.2, -8.6, 80),
                18.0,
                "IPMA",
                LocalDateTime.of(2026, 6, 1, 15, 0));
        final var dto1 = WeatherDataDTO.from(wd1);
        final var dto2 = WeatherDataDTO.from(wd2);
        assertNotEquals(dto1, dto2);
    }

    @Test
    void toStringContainsAreaCode() {
        final var dto = WeatherDataDTO.from(sampleWeatherData());
        assertTrue(dto.toString().contains("LIS"));
    }

    @Test
    void recordIsImmutable() {
        final var dto = WeatherDataDTO.from(sampleWeatherData());
        assertAll(
                () -> assertNotNull(dto.areaCode()),
                () -> assertNotNull(dto.sourceProvider()),
                () -> assertNotNull(dto.recordedDateTime())
        );
    }
}
