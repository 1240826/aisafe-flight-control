package eapli.aisafe.weatherdata.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class WeatherDataTest {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @ParameterizedTest(name = "{0}")
    @MethodSource("csvTestData")
    void ensureWeatherDataInvariants(
            final String testCaseId,
            final String areaCodeStr,
            final double windSpeed,
            final int windDir,
            final double windLat,
            final double windLon,
            final int windAlt,
            final double temperature,
            final String sourceProvider,
            final String dateStr,
            final String timeStr,
            final boolean expectValid) {
        if (expectValid) {
            final var areaCode = AreaCode.valueOf(areaCodeStr);
            final var wind = new WindCondition(windSpeed, windDir, windLat, windLon, windAlt);
            final var dateTime = parseDateTime(dateStr, timeStr);
            final var wd = assertDoesNotThrow(() ->
                    new WeatherData(areaCode, wind, temperature, sourceProvider, dateTime));
            assertEquals(areaCode, wd.areaCode());
            assertEquals(temperature, wd.temperatureCelsius(), 0.001);
            assertEquals(sourceProvider.trim(), wd.sourceProvider());
            assertEquals(dateTime, wd.recordedDateTime());
        } else {
            final var areaCode = areaCodeStr.isEmpty() ? null : AreaCode.valueOf(areaCodeStr);
            final var dateTime = tryParseDateTime(dateStr, timeStr);
            if (dateTime == null) {
                return;
            }
            final WindCondition wind;
            try {
                wind = new WindCondition(windSpeed, windDir, windLat, windLon, windAlt);
            } catch (final Exception e) {
                return;
            }
            final var finalAreaCode = areaCode;
            final var finalDateTime = dateTime;
            assertThrows(RuntimeException.class, () ->
                    new WeatherData(finalAreaCode, wind, temperature, sourceProvider, finalDateTime));
        }
    }

    @Test
    void ensureNullAreaCodeIsRejected() {
        final var wind = new WindCondition(15.5, 270, 38.7, -9.1, 1000);
        assertThrows(Exception.class, () -> new WeatherData(
                null, wind, 18.5, "IPMA", LocalDateTime.of(2026, 5, 14, 10, 0)));
    }

    @Test
    void ensureNullWindConditionIsRejected() {
        assertThrows(Exception.class, () -> new WeatherData(
                AreaCode.valueOf("LPPC"), null, 18.5, "IPMA", LocalDateTime.of(2026, 5, 14, 10, 0)));
    }

    @Test
    void ensureNullSourceProviderIsRejected() {
        final var wind = new WindCondition(15.5, 270, 38.7, -9.1, 1000);
        assertThrows(Exception.class, () -> new WeatherData(
                AreaCode.valueOf("LPPC"), wind, 18.5, null, LocalDateTime.of(2026, 5, 14, 10, 0)));
    }

    @Test
    void ensureNullRecordedDateTimeIsRejected() {
        final var wind = new WindCondition(15.5, 270, 38.7, -9.1, 1000);
        assertThrows(Exception.class, () -> new WeatherData(
                AreaCode.valueOf("LPPC"), wind, 18.5, "IPMA", null));
    }

    @Test
    void ensureToStringContainsAreaCode() {
        final var wd = new WeatherData(
                AreaCode.valueOf("LPPC"),
                new WindCondition(15.5, 270, 38.7, -9.1, 1000),
                18.5, "IPMA", LocalDateTime.of(2026, 5, 14, 10, 0));
        assertTrue(wd.toString().contains("LPPC"));
    }

    @Test
    void ensureIdentityIsNullBeforePersistence() {
        final var wd = new WeatherData(
                AreaCode.valueOf("LPPC"),
                new WindCondition(15.5, 270, 38.7, -9.1, 1000),
                18.5, "IPMA", LocalDateTime.of(2026, 5, 14, 10, 0));
        assertNull(wd.identity());
    }

    @Test
    void ensureNegativeTemperatureIsAccepted() {
        final var wd = new WeatherData(
                AreaCode.valueOf("BIRD"),
                new WindCondition(30.0, 180, 65.0, -18.0, 500),
                -35.0, "Isavia", LocalDateTime.of(2026, 5, 14, 10, 0));
        assertEquals(-35.0, wd.temperatureCelsius(), 0.001);
    }

    private static Stream<Arguments> csvTestData() {
        final var rows = new ArrayList<Arguments>();
        try (var reader = new BufferedReader(new InputStreamReader(
                WeatherDataTest.class.getResourceAsStream("/us042/weather_data_test.csv"),
                StandardCharsets.UTF_8))) {
            final var lines = reader.lines().toList();
            for (final var line : lines) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("testCaseId")) continue;
                final var parts = line.split(";", -1);
                if (parts.length < 12) continue;
                rows.add(Arguments.of(
                        parts[0].trim(),
                        parts[1].trim(),
                        Double.parseDouble(parts[2].trim()),
                        Integer.parseInt(parts[3].trim()),
                        Double.parseDouble(parts[4].trim()),
                        Double.parseDouble(parts[5].trim()),
                        Integer.parseInt(parts[6].trim()),
                        Double.parseDouble(parts[7].trim()),
                        parts[8].trim(),
                        parts[9].trim(),
                        parts[10].trim(),
                        Boolean.parseBoolean(parts[11].trim())
                ));
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to load CSV test data", e);
        }
        return rows.stream();
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us043/weather_data_test.csv", numLinesToSkip = 1)
    void ensureWeatherDataCsvInvariants(final String testCaseId, final String areaCodeStr,
                                         final double windSpeed, final int windDir,
                                         final double windLat, final double windLon,
                                         final int windAlt, final double temperature,
                                         final String sourceProvider, final String dateStr,
                                         final String timeStr, final boolean expectedValid) {
        if (expectedValid) {
            assertDoesNotThrow(() -> new WeatherData(
                    AreaCode.valueOf(areaCodeStr),
                    new WindCondition(windSpeed, windDir, windLat, windLon, windAlt),
                    temperature, sourceProvider, parseDateTime(dateStr, timeStr)));
        } else {
            assertThrows(Exception.class, () -> new WeatherData(
                    areaCodeStr == null || areaCodeStr.isBlank() ? null : AreaCode.valueOf(areaCodeStr),
                    new WindCondition(windSpeed, windDir, windLat, windLon, windAlt),
                    temperature, sourceProvider,
                    timeStr == null || timeStr.isBlank() ? null : parseDateTime(dateStr, timeStr)));
        }
    }

    private static LocalDateTime parseDateTime(final String dateStr, final String timeStr) {
        final var date = LocalDate.parse(dateStr, DATE_FMT);
        final var time = LocalTime.parse(timeStr, TIME_FMT);
        return LocalDateTime.of(date, time);
    }

    private static LocalDateTime tryParseDateTime(final String dateStr, final String timeStr) {
        try {
            return parseDateTime(dateStr, timeStr);
        } catch (final DateTimeParseException e) {
            return null;
        }
    }
}
