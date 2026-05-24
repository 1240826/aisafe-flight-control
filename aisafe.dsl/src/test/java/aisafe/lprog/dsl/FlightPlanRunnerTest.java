package aisafe.lprog.dsl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FlightPlanRunner — lexical and syntactic validation.
 * Follows AAA convention (Arrange / Act / Assert).
 *
 * Schedule format:
 *   Charter departure : datetime: YYYY-MM-DDTHH:MM+TZ;
 *   Regular departure : day: DayOfWeek; datetime: YYYY-MM-DDTHH:MM+TZ;
 *   All arrivals      : datetime: YYYY-MM-DDTHH:MM+TZ;  (destination timezone)
 */
class FlightPlanRunnerTest {

    // ---- helpers ----

    private Path resource(String filename) throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource("examples/" + filename),
                "Resource not found: " + filename
        ).toURI());
    }

    private boolean parse(String content) throws IOException {
        Path tmp = Files.createTempFile("test_", ".flightplan");
        try {
            Files.writeString(tmp, content);
            return FlightPlanRunner.run(tmp, false);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // VALID — files from disk

    @Test
    @DisplayName("Valid direct flight (file) should pass")
    void validDirectFlightFile() throws IOException, URISyntaxException {
        assertTrue(FlightPlanRunner.run(resource("valid_direct_flight.flightplan"), true));
    }

    @Test
    @DisplayName("Valid multi-leg charter (file) should pass")
    void validMultiLegFile() throws IOException, URISyntaxException {
        assertTrue(FlightPlanRunner.run(resource("valid_multi_leg.flightplan"), false));
    }

    @Test
    @DisplayName("Valid regular multi-leg flight with day-of-week schedule (file) should pass")
    void validRegularMultiLegFile() throws IOException, URISyntaxException {
        assertTrue(FlightPlanRunner.run(resource("valid_regular_multi_leg.flightplan"), false));
    }

    // VALID — inline cases

    @Test
    @DisplayName("ICAO codes (4 uppercase letters) should be accepted")
    void icaoAirportCodes() throws IOException {
        String content = """
                flight TP001 : charter {
                    route { origin: LPPT; destination: EDDF; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: LPPT; datetime: 2026-05-10T06:00+01:00; }
                        arrival   { airport: EDDF; datetime: 2026-05-10T09:00+02:00; }
                        fuel      { quantity: 12000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359); to: (50.0333, 8.5706);
                            altitudes: [10000 m]; wind: (90, 15 m/s);
                        }
                    }
                }
                """;
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Keywords in UPPERCASE should be accepted (case-insensitive)")
    void keywordsUpperCase() throws IOException {
        String content = """
                FLIGHT TP002 : REGULAR {
                    ROUTE { ORIGIN: LIS; DESTINATION: LHR; }
                    AIRCRAFT : CS-TUB;
                    PILOT    : P12345;
                    LEG L1 {
                        DEPARTURE { AIRPORT: LIS; DAY: Monday; DATETIME: 2026-05-18T08:00+01:00; }
                        ARRIVAL   { AIRPORT: LHR; DATETIME: 2026-05-18T10:00+01:00; }
                        FUEL      { QUANTITY: 15000 kg; }
                        SEGMENT S1 {
                            FROM: (38.7813, -9.1359); TO: (51.4775, -0.4614);
                            ALTITUDES: [10000 m]; WIND: (270, 15 m/s);
                        }
                    }
                }
                """;
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Keywords in mixed case should be accepted (case-insensitive)")
    void keywordsMixedCase() throws IOException {
        String content = """
                Flight TP003 : Regular {
                    Route { Origin: LIS; Destination: LHR; }
                    Aircraft : CS-TUB;
                    Pilot    : P12345;
                    Leg L1 {
                        Departure { Airport: LIS; Day: Monday; Datetime: 2026-05-18T08:00+01:00; }
                        Arrival   { Airport: LHR; Datetime: 2026-05-18T10:00+01:00; }
                        Fuel      { Quantity: 15000 kg; }
                        Segment S1 {
                            From: (38.7813, -9.1359); To: (51.4775, -0.4614);
                            Altitudes: [10000 m]; Wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Charter flight type should be accepted")
    void charterFlightType() throws IOException {
        String content = """
                flight TP004 : charter {
                    route { origin: OPO; destination: WAW; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: OPO; datetime: 2026-06-15T06:00+01:00; }
                        arrival   { airport: WAW; datetime: 2026-06-15T10:00+02:00; }
                        fuel      { quantity: 18000 kg; }
                        segment S1 {
                            from: (41.2481, -8.6814); to: (52.1657, 20.9671);
                            altitudes: [11000 m WIDTH 80 m]; wind: (45, 20 m/s);
                        }
                    }
                }
                """;
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Multiple segments in one leg should be accepted")
    void multipleSegments() throws IOException {
        String content = """
                flight TP005 : charter {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: LIS; datetime: 2026-05-10T08:00+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-10T10:00+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359); to: (45.0, -5.0);
                            altitudes: [10000 m]; wind: (270, 15 m/s);
                        }
                        segment S2 {
                            from: (45.0, -5.0); to: (51.4775, -0.4614);
                            altitudes: [11000 m WIDTH 60 m]; wind: (280, 18 m/s);
                        }
                    }
                }
                """;
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Fuel in litres (l) should be accepted")
    void fuelInLitres() throws IOException {
        String content = """
                flight TP006 : charter {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: LIS; datetime: 2026-05-10T08:00+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-10T10:00+01:00; }
                        fuel      { quantity: 18000 l; }
                        segment S1 {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [35000 ft]; wind: (270, 250 kt);
                        }
                    }
                }
                """;
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Comments (// and /* */) should be ignored")
    void commentsIgnored() throws IOException {
        String content = """
                // Flight plan comment
                flight TP007 : charter { /* charter flight */
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: LIS; datetime: 2026-05-10T08:00+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-10T10:00+01:00; } // arrival
                        fuel      { quantity: 15000 kg; }
                        /* segment below */
                        segment S1 {
                            from: (38.7813, -9.1359); // Lisbon
                            to:   (51.4775, -0.4614);
                            altitudes: [10000 m]; wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Negative coordinates should be accepted")
    void negativeCoordinates() throws IOException {
        // São Paulo (UTC-3) to Lisbon (UTC+1)
        String content = """
                flight TP008 : charter {
                    route { origin: GRU; destination: LIS; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: GRU; datetime: 2026-05-10T01:00-03:00; }
                        arrival   { airport: LIS; datetime: 2026-05-10T14:00+01:00; }
                        fuel      { quantity: 50000 kg; }
                        segment S1 {
                            from: (-23.4356, -46.4731); to: (38.7813, -9.1359);
                            altitudes: [12000 m]; wind: (90, 30 m/s);
                        }
                    }
                }
                """;
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Timestamp with seconds (YYYY-MM-DDTHH:MM:SS+TZ) should be accepted")
    void timeWithSeconds() throws IOException {
        // TIMESTAMP_LITERAL supports optional seconds: HH:MM[:SS]
        String content = """
                flight TP009 : charter {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: LIS; datetime: 2026-05-10T08:30:45+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-10T10:45:00+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [10000 m]; wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Multiple altitude slots should be accepted")
    void multipleAltitudeSlots() throws IOException {
        String content = """
                flight TP010 : charter {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: LIS; datetime: 2026-05-10T08:00+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-10T10:00+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [8000 m, 9000 m WIDTH 50 m, 10000 m, 11000 m WIDTH 80 m];
                            wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Regular flight with day + datetime departure should be accepted")
    void regularFlightWithDayDatetime() throws IOException {
        // Regular: day-of-week identifies the weekly pattern; datetime gives exact date + timezone
        String content = """
                flight TP017 : regular {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: LIS; day: Wednesday; datetime: 2026-05-20T08:30+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-20T10:45+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [10000 m]; wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        assertTrue(parse(content));
    }

    // INVALID — should fail with errors

    @Test
    @DisplayName("Invalid file with multiple errors (from disk) should fail")
    void invalidFileFromDisk() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_flight.flightplan"), false));
    }

    @Test
    @DisplayName("[D1] Unknown flight type 'cargo' (file) should fail")
    void invalidBadFlightTypeFile() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_bad_flight_type.flightplan"), false));
    }

    @Test
    @DisplayName("[D2] Missing route block (file) should fail")
    void invalidMissingRouteFile() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_missing_route.flightplan"), false));
    }

    @Test
    @DisplayName("[D3] Missing departure block inside leg (file) should fail")
    void invalidMissingDepartureFile() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_missing_departure.flightplan"), false));
    }

    @Test
    @DisplayName("[D4] Missing arrival block inside leg (file) should fail")
    void invalidMissingArrivalFile() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_missing_arrival.flightplan"), false));
    }

    @Test
    @DisplayName("[D5] Missing fuel block inside leg (file) should fail")
    void invalidMissingFuelFile() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_missing_fuel.flightplan"), false));
    }

    @Test
    @DisplayName("[D6] Missing segment block inside leg (file) should fail")
    void invalidMissingSegmentFile() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_missing_segment.flightplan"), false));
    }

    @Test
    @DisplayName("[D7] Unclosed leg brace (file) should fail")
    void invalidUnclosedBraceFile() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_unclosed_brace.flightplan"), false));
    }

    @Test
    @DisplayName("[D8] Wrong datetime format DD-MM-YYYY instead of YYYY-MM-DD (file) should fail")
    void invalidBadDateFormatFile() throws IOException, URISyntaxException {
        // TIMESTAMP_LITERAL requires 4-digit year first; '10' (2 digits) → parser error
        assertFalse(FlightPlanRunner.run(resource("invalid_bad_date_format.flightplan"), false));
    }

    @Test
    @DisplayName("[D9] Using 'time:' instead of 'datetime:' in regular departure (file) should fail")
    void invalidBadTimeFormatFile() throws IOException, URISyntaxException {
        // Regular departure: grammar expects DATETIME after DAY+DAY_LITERAL, not TIME
        assertFalse(FlightPlanRunner.run(resource("invalid_bad_time_format.flightplan"), false));
    }

    @Test
    @DisplayName("[D10] Airport code with only 2 letters (file) should fail")
    void invalidBadAirportCodeFile() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_bad_airport_code.flightplan"), false));
    }

    @Test
    @DisplayName("[D11] Unknown token '#' (file) should fail with lexer error")
    void invalidUnknownTokenFile() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_unknown_token.flightplan"), false));
    }

    @Test
    @DisplayName("[D12] Missing semicolon after field value (file) should fail")
    void invalidMissingSemicolonFile() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_missing_semicolon.flightplan"), false));
    }

    @Test
    @DisplayName("Missing semicolon in charter departure should produce parser error")
    void missingSemicolon() throws IOException {
        String content = """
                flight TP011 : charter {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: LIS; datetime: 2026-05-10T08:30+01:00 }
                        arrival   { airport: LHR; datetime: 2026-05-10T10:45+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [10000 m]; wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("Invalid token '@' should produce lexer error")
    void invalidToken() throws IOException {
        String content = """
                flight @TP012 : regular {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: LIS; day: Monday; datetime: 2026-05-18T08:30+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-18T10:45+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [10000 m]; wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("Missing route block should produce parser error")
    void missingRouteBlock() throws IOException {
        String content = """
                flight TP013 : charter {
                    leg L1 {
                        departure { airport: LIS; datetime: 2026-05-10T08:30+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-10T10:45+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [10000 m]; wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("Missing segment block should produce parser error")
    void missingSegmentBlock() throws IOException {
        String content = """
                flight TP014 : charter {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: LIS; datetime: 2026-05-10T08:30+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-10T10:45+01:00; }
                        fuel      { quantity: 15000 kg; }
                    }
                }
                """;
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("Invalid flight type 'cargo' should produce parser error")
    void invalidFlightType() throws IOException {
        String content = """
                flight TP015 : cargo {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: LIS; day: Monday; datetime: 2026-05-18T08:30+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-18T10:45+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [10000 m]; wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("Missing closing brace should produce parser error")
    void missingClosingBrace() throws IOException {
        String content = """
                flight TP016 : charter {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg L1 {
                        departure { airport: LIS; datetime: 2026-05-10T08:30+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-10T10:45+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [10000 m]; wind: (270, 15 m/s);
                        }
                    }
                // missing closing brace for flight block
                """;
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("Empty file should produce parser error")
    void emptyFile() throws IOException {
        assertFalse(parse(""));
    }
}
