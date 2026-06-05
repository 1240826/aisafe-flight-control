package eapli.aisafe.flightplan.application;

import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flightplan.domain.FlightPlan;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanExporterTest {

    private static final LocalDateTime DEP_TIME = LocalDateTime.of(2026, 6, 2, 10, 0);

    private static final String VALID_DSL =
            "flight TP3000 : charter {\n" +
            "    route { origin: LIS; destination: CDG; }\n" +
            "    aircraft: CS-TUB;\n" +
            "    pilot: P12345;\n" +
            "    leg {\n" +
            "        departure { airport: LIS; datetime: 2026-06-02T10:00+01:00; }\n" +
            "        arrival   { airport: CDG; datetime: 2026-06-02T13:30+02:00; }\n" +
            "        fuel      { quantity: 8000 kg; }\n" +
            "        segment {\n" +
            "            from      : (38.7813, -9.1359);\n" +
            "            to        : (49.0097, 2.5479);\n" +
            "            altitudes : [10000 m WIDTH 60 m];\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private final FlightPlanExporter exporter = new FlightPlanExporter();

    @Test
    void ensureExportsStructuredJsonForValidDsl() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), VALID_DSL);
        final var json = exporter.exportForSimulator(fp);

        assertTrue(json.startsWith("["), "Structured JSON should start with [");
        assertTrue(json.contains("\"ID\": \"TP3000\""), "Should contain flight ID from DSL");
        assertTrue(json.contains("\"DepartureTime\": \"10:00\""), "Should contain departure time");
        assertTrue(json.contains("\"Segments\":"), "Should contain segments");
        assertTrue(json.contains("\"Flight Profile\":"), "Should contain flight profile");
    }

    @Test
    void ensureExportsValidStructuredJson() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), VALID_DSL);
        final var json = exporter.exportForSimulator(fp);

        assertTrue(json.startsWith("["), "Should start with array bracket");
        assertTrue(json.endsWith("]\n"), "Should end with array bracket");
    }

    @Test
    void ensureFallbackForInvalidDsl() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "departure LIS;");
        final var json = exporter.exportForSimulator(fp);

        assertTrue(json.startsWith("["), "Fallback JSON should start with [");
        assertTrue(json.contains("\"ID\": \"FP001\""), "Should contain flight plan ID");
        assertTrue(json.contains("\"Leg\":"), "Should contain Leg array");
        assertTrue(json.contains("\"Segments\":"), "Should contain Segments array");
    }

    @Test
    void ensureFallbackHasCoordinates() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00; flight XPF123;");
        final var json = exporter.exportForSimulator(fp);

        assertTrue(json.contains("\"Latitude\": 38.774"), "Should contain LIS coordinates");
        assertTrue(json.contains("\"Latitude\": 41.248"), "Should contain OPO coordinates");
        assertTrue(json.contains("\"Mode\": \"climb\""), "Should have climb mode");
        assertTrue(json.contains("\"Mode\": \"cruise\""), "Should have cruise mode");
        assertTrue(json.contains("\"Mode\": \"descend\""), "Should have descend mode");
    }
}
