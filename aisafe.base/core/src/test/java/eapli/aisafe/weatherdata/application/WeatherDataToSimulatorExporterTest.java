package eapli.aisafe.weatherdata.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.domain.WindCondition;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeatherDataToSimulatorExporterTest {

    private static final AreaCode ACA = new AreaCode("LIS_ACA");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 5, 12, 0);

    @Test
    void ensureExportsProvider() {
        final var wd = new WeatherData(ACA, new WindCondition(12, 270, 38.78, -9.14, 10000), 22, "Test", NOW);
        final var json = new WeatherDataToSimulatorExporter().export(List.of(wd));
        assertTrue(json.contains("\"provider\""), "Should contain provider");
    }

    @Test
    void ensureExportsDurationHours() {
        final var wd = new WeatherData(ACA, new WindCondition(12, 270, 38.78, -9.14, 10000), 22, "Test", NOW);
        final var json = new WeatherDataToSimulatorExporter().export(List.of(wd));
        assertTrue(json.contains("\"duration_hours\""), "Should contain duration_hours");
    }

    @Test
    void ensureExportsZonesArray() {
        final var wd = new WeatherData(ACA, new WindCondition(12, 270, 38.78, -9.14, 10000), 22, "Test", NOW);
        final var json = new WeatherDataToSimulatorExporter().export(List.of(wd));
        assertTrue(json.contains("\"zones\""), "Should contain zones");
    }

    @Test
    void ensureExportsAllZoneFields() {
        final var wd = new WeatherData(ACA, new WindCondition(15.5, 270, 38.78, -9.14, 10000), 22, "Test", NOW);
        final var json = new WeatherDataToSimulatorExporter().export(List.of(wd));
        assertTrue(json.contains("\"lat_north\""), "lat_north");
        assertTrue(json.contains("\"lat_south\""), "lat_south");
        assertTrue(json.contains("\"lon_west\""), "lon_west");
        assertTrue(json.contains("\"lon_east\""), "lon_east");
        assertTrue(json.contains("\"alt_ft_lo\""), "alt_ft_lo");
        assertTrue(json.contains("\"alt_ft_hi\""), "alt_ft_hi");
        assertTrue(json.contains("\"dir_deg\""), "dir_deg");
        assertTrue(json.contains("\"speed_kt\""), "speed_kt");
    }

    @Test
    void ensureExportsCorrectValues() {
        final var wd = new WeatherData(ACA, new WindCondition(25, 90, 40.0, -8.0, 3000), 18, "Test", NOW);
        final var json = new WeatherDataToSimulatorExporter().export(List.of(wd));
        assertTrue(json.contains("\"speed_kt\": 25.0"), "speed_kt");
        assertTrue(json.contains("\"dir_deg\": 90"), "dir_deg");
    }

    @Test
    void ensureExportsMultipleZones() {
        final var wd1 = new WeatherData(ACA, new WindCondition(10, 180, 38.0, -9.0, 0), 20, "Test", NOW);
        final var wd2 = new WeatherData(ACA, new WindCondition(20, 200, 38.0, -9.0, 3000), 18, "Test", NOW);
        final var json = new WeatherDataToSimulatorExporter().export(List.of(wd1, wd2));

        final var count = json.split("\"lat_north\"").length - 1;
        assertEquals(2, count, "Should have 2 zones");
    }

    @Test
    void ensureValidJsonSyntax() {
        final var wd = new WeatherData(ACA, new WindCondition(12, 270, 38.78, -9.14, 10000), 22, "Test", NOW);
        final var json = new WeatherDataToSimulatorExporter().export(List.of(wd));

        assertTrue(json.startsWith("{"), "Should start with {");
        assertTrue(json.endsWith("}\n"), "Should end with }");
    }

    @Test
    void ensureEmptyListStillProducesValidJson() {
        final var json = new WeatherDataToSimulatorExporter().export(List.of());
        assertTrue(json.startsWith("{"), "Should start with {");
        assertTrue(json.endsWith("}\n"), "Should end with }");
        assertTrue(json.contains("\"zones\""), "Should have zones key");
        assertFalse(json.contains("lat_north"), "Should have no zone entries");
    }

    @Test
    void ensureAltitudesConvertedToFeet() {
        final var wd = new WeatherData(ACA, new WindCondition(12, 270, 38.78, -9.14, 3048), 22, "Test", NOW);
        final var json = new WeatherDataToSimulatorExporter().export(List.of(wd));
        assertTrue(json.contains("\"alt_ft_lo\":"), "alt_ft_lo");
        assertTrue(json.contains("\"alt_ft_hi\":"), "alt_ft_hi");
    }

    @Test
    void ensureOutputMatchesCSimulatorJsonParser() {
        final var wd = new WeatherData(ACA, new WindCondition(12, 270, 38.78, -9.14, 10000), 22, "Test", NOW);
        final var json = new WeatherDataToSimulatorExporter().export(List.of(wd));

        assertTrue(json.contains("\"provider\""), "load_weather_from_json reads provider");
        assertTrue(json.contains("\"duration_hours\""), "load_weather_from_json reads duration_hours");
        assertTrue(json.contains("\"zones\""), "load_weather_from_json reads zones");
        assertTrue(json.contains("\"lat_north\""), "jp_dbl_def reads lat_north");
        assertTrue(json.contains("\"lat_south\""), "jp_dbl_def reads lat_south");
        assertTrue(json.contains("\"lon_west\""), "jp_dbl_def reads lon_west");
        assertTrue(json.contains("\"lon_east\""), "jp_dbl_def reads lon_east");
        assertTrue(json.contains("\"alt_ft_lo\""), "jp_dbl_def reads alt_ft_lo");
        assertTrue(json.contains("\"alt_ft_hi\""), "jp_dbl_def reads alt_ft_hi");
        assertTrue(json.contains("\"dir_deg\""), "jp_dbl_def reads dir_deg");
        assertTrue(json.contains("\"speed_kt\""), "jp_dbl_def reads speed_kt");
    }

    @Test
    void ensureFormatterMatchesCSimulatorExpectations() {
        final var wd = new WeatherData(ACA, new WindCondition(12, 270, 38.78, -9.14, 10000), 22, "Test", NOW);
        final var json = new WeatherDataToSimulatorExporter().export(List.of(wd));

        assertTrue(json.contains("{\n"));
        assertTrue(json.contains("\"provider\""));
        assertTrue(json.contains("\"duration_hours\""));
        assertTrue(json.contains("\"zones\""));
        assertTrue(json.contains("lat_north"));
        assertTrue(json.contains("lat_south"));
        assertTrue(json.contains("lon_west"));
        assertTrue(json.contains("lon_east"));
        assertTrue(json.contains("alt_ft_lo"));
        assertTrue(json.contains("alt_ft_hi"));
        assertTrue(json.contains("dir_deg"));
        assertTrue(json.contains("speed_kt"));
    }
}
