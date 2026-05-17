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
 * Covers:
 *  - Valid files from disk
 *  - Valid inline cases: ICAO codes, case-insensitive keywords,
 *    multiple segments, various units, comments, negative coords, HH:MM:SS
 *  - Invalid cases: missing semicolon, invalid token, missing route,
 *    missing segment, wrong flight type, missing brace, empty file
 */
class FlightPlanRunnerTest {

    // ---- helpers ----

    private Path resource(String filename) throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource("examples/" + filename),
                "Resource not found: " + filename
        ).toURI());
    }

    /** Writes content to a temp file, runs the parser, deletes the file. */
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
        // Arrange
        Path file = resource("valid_direct_flight.flightplan");
        // Act
        boolean result = FlightPlanRunner.run(file, true);
        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Valid multi-leg charter (file) should pass")
    void validMultiLegFile() throws IOException, URISyntaxException {
        // Arrange
        Path file = resource("valid_multi_leg.flightplan");
        // Act
        boolean result = FlightPlanRunner.run(file, false);
        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Valid regular multi-leg flight with day-of-week schedule (file) should pass")
    void validRegularMultiLegFile() throws IOException, URISyntaxException {
        // Arrange
        Path file = resource("valid_regular_multi_leg.flightplan");
        // Act
        boolean result = FlightPlanRunner.run(file, false);
        // Assert
        assertTrue(result);
    }

    // VALID — inline cases

    @Test
    @DisplayName("ICAO codes (4 uppercase letters) should be accepted")
    void icaoAirportCodes() throws IOException {
        // Arrange
        String content = """
                flight TP001 : charter {
                    route { origin: LPPT; destination: EDDF; }
                    leg L1 {
                        departure { airport: LPPT; date: 2026-05-10; time: 06:00; }
                        arrival   { airport: EDDF; time: 09:00; }
                        fuel      { quantity: 12000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359);
                            to:   (50.0333, 8.5706);
                            altitudes: [10000 m];
                            wind: (90, 15 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Keywords in UPPERCASE should be accepted (case-insensitive)")
    void keywordsUpperCase() throws IOException {
        // Arrange
        String content = """
                FLIGHT TP002 : REGULAR {
                    ROUTE { ORIGIN: LIS; DESTINATION: LHR; }
                    LEG L1 {
                        DEPARTURE { AIRPORT: LIS; DAY: Monday; TIME: 08:00; }
                        ARRIVAL   { AIRPORT: LHR; TIME: 10:00; }
                        FUEL      { QUANTITY: 15000 kg; }
                        SEGMENT S1 {
                            FROM: (38.7813, -9.1359);
                            TO:   (51.4775, -0.4614);
                            ALTITUDES: [10000 m];
                            WIND: (270, 15 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Keywords in mixed case should be accepted (case-insensitive)")
    void keywordsMixedCase() throws IOException {
        // Arrange
        String content = """
                Flight TP003 : Regular {
                    Route { Origin: LIS; Destination: LHR; }
                    Leg L1 {
                        Departure { Airport: LIS; Day: Monday; Time: 08:00; }
                        Arrival   { Airport: LHR; Time: 10:00; }
                        Fuel      { Quantity: 15000 kg; }
                        Segment S1 {
                            From: (38.7813, -9.1359);
                            To:   (51.4775, -0.4614);
                            Altitudes: [10000 m];
                            Wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Charter flight type should be accepted")
    void charterFlightType() throws IOException {
        // Arrange
        String content = """
                flight TP004 : charter {
                    route { origin: OPO; destination: WAW; }
                    leg L1 {
                        departure { airport: OPO; date: 2026-06-15; time: 06:00; }
                        arrival   { airport: WAW; time: 10:00; }
                        fuel      { quantity: 18000 kg; }
                        segment S1 {
                            from: (41.2481, -8.6814);
                            to:   (52.1657, 20.9671);
                            altitudes: [11000 m WIDTH 80 m];
                            wind: (45, 20 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Multiple segments in one leg should be accepted")
    void multipleSegments() throws IOException {
        // Arrange
        String content = """
                flight TP005 : charter {
                    route { origin: LIS; destination: LHR; }
                    leg L1 {
                        departure { airport: LIS; date: 2026-05-10; time: 08:00; }
                        arrival   { airport: LHR; time: 10:00; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359);
                            to:   (45.0, -5.0);
                            altitudes: [10000 m];
                            wind: (270, 15 m/s);
                        }
                        segment S2 {
                            from: (45.0, -5.0);
                            to:   (51.4775, -0.4614);
                            altitudes: [11000 m WIDTH 60 m];
                            wind: (280, 18 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Fuel in litres (l) should be accepted")
    void fuelInLitres() throws IOException {
        // Arrange
        String content = """
                flight TP006 : charter {
                    route { origin: LIS; destination: LHR; }
                    leg L1 {
                        departure { airport: LIS; date: 2026-05-10; time: 08:00; }
                        arrival   { airport: LHR; time: 10:00; }
                        fuel      { quantity: 18000 l; }
                        segment S1 {
                            from: (38.7813, -9.1359);
                            to:   (51.4775, -0.4614);
                            altitudes: [35000 ft];
                            wind: (270, 250 kt);
                        }
                    }
                }
                """;
        // Act + Assert
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Comments (// and /* */) should be ignored")
    void commentsIgnored() throws IOException {
        // Arrange
        String content = """
                // This is a flight plan comment
                flight TP007 : charter { /* charter or regular */
                    route { origin: LIS; destination: LHR; }
                    leg L1 {
                        departure { airport: LIS; date: 2026-05-10; time: 08:00; }
                        arrival   { airport: LHR; time: 10:00; } // arrival block
                        fuel      { quantity: 15000 kg; }
                        /* segment block below */
                        segment S1 {
                            from: (38.7813, -9.1359); // Lisbon
                            to:   (51.4775, -0.4614);
                            altitudes: [10000 m];
                            wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Negative coordinates should be accepted")
    void negativeCoordinates() throws IOException {
        // Arrange — São Paulo to Lisbon
        String content = """
                flight TP008 : charter {
                    route { origin: GRU; destination: LIS; }
                    leg L1 {
                        departure { airport: GRU; date: 2026-05-10; time: 01:00; }
                        arrival   { airport: LIS; time: 14:00; }
                        fuel      { quantity: 50000 kg; }
                        segment S1 {
                            from: (-23.4356, -46.4731);
                            to:   (38.7813, -9.1359);
                            altitudes: [12000 m];
                            wind: (90, 30 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Time with seconds (HH:MM:SS) should be accepted")
    void timeWithSeconds() throws IOException {
        // Arrange
        String content = """
                flight TP009 : charter {
                    route { origin: LIS; destination: LHR; }
                    leg L1 {
                        departure { airport: LIS; date: 2026-05-10; time: 08:30:45; }
                        arrival   { airport: LHR; time: 10:45:00; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359);
                            to:   (51.4775, -0.4614);
                            altitudes: [10000 m];
                            wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertTrue(parse(content));
    }

    @Test
    @DisplayName("Multiple altitude slots should be accepted")
    void multipleAltitudeSlots() throws IOException {
        // Arrange
        String content = """
                flight TP010 : charter {
                    route { origin: LIS; destination: LHR; }
                    leg L1 {
                        departure { airport: LIS; date: 2026-05-10; time: 08:00; }
                        arrival   { airport: LHR; time: 10:00; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359);
                            to:   (51.4775, -0.4614);
                            altitudes: [8000 m, 9000 m WIDTH 50 m, 10000 m, 11000 m WIDTH 80 m];
                            wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertTrue(parse(content));
    }

    // INVALID — should fail with errors

    @Test
    @DisplayName("Invalid file with multiple errors (from disk) should fail")
    void invalidFileFromDisk() throws IOException, URISyntaxException {
        // Arrange
        Path file = resource("invalid_flight.flightplan");
        // Act
        boolean result = FlightPlanRunner.run(file, false);
        // Assert
        assertFalse(result);
    }


    // INVALID — specific files
    // Each file isolates a single syntactic or lexical rule violation

    @Test
    @DisplayName("[D1] Unknown flight type 'cargo' (file) should fail")
    void invalidBadFlightTypeFile() throws IOException, URISyntaxException {
        // Rule: flightType accepts only REGULAR | CHARTER
        assertFalse(FlightPlanRunner.run(resource("invalid_bad_flight_type.flightplan"), false));
    }

    @Test
    @DisplayName("[D2] Missing route block (file) should fail")
    void invalidMissingRouteFile() throws IOException, URISyntaxException {
        // Rule: flightDecl requires routeDecl before legDecl+
        assertFalse(FlightPlanRunner.run(resource("invalid_missing_route.flightplan"), false));
    }

    @Test
    @DisplayName("[D3] Missing departure block inside leg (file) should fail")
    void invalidMissingDepartureFile() throws IOException, URISyntaxException {
        // Rule: legDecl requires departureDecl as its first sub-block
        assertFalse(FlightPlanRunner.run(resource("invalid_missing_departure.flightplan"), false));
    }

    @Test
    @DisplayName("[D4] Missing arrival block inside leg (file) should fail")
    void invalidMissingArrivalFile() throws IOException, URISyntaxException {
        // Rule: legDecl requires arrivalDecl after departureDecl
        assertFalse(FlightPlanRunner.run(resource("invalid_missing_arrival.flightplan"), false));
    }

    @Test
    @DisplayName("[D5] Missing fuel block inside leg (file) should fail")
    void invalidMissingFuelFile() throws IOException, URISyntaxException {
        // Rule: legDecl requires fuelDecl before segmentDecl+
        assertFalse(FlightPlanRunner.run(resource("invalid_missing_fuel.flightplan"), false));
    }

    @Test
    @DisplayName("[D6] Missing segment block inside leg (file) should fail")
    void invalidMissingSegmentFile() throws IOException, URISyntaxException {
        // Rule: legDecl requires segmentDecl+ (one or more)
        assertFalse(FlightPlanRunner.run(resource("invalid_missing_segment.flightplan"), false));
    }

    @Test
    @DisplayName("[D7] Unclosed leg brace (file) should fail")
    void invalidUnclosedBraceFile() throws IOException, URISyntaxException {
        // Rule: every opened '{' must have a matching '}'
        assertFalse(FlightPlanRunner.run(resource("invalid_unclosed_brace.flightplan"), false));
    }

    @Test
    @DisplayName("[D8] Wrong date format DD-MM-YYYY instead of YYYY-MM-DD (file) should fail")
    void invalidBadDateFormatFile() throws IOException, URISyntaxException {
        // Rule: DATE_LITERAL requires exactly 4-2-2 digit groups separated by '-'
        // '10' alone is tokenised as NUMBER, not DATE_LITERAL → parser error
        assertFalse(FlightPlanRunner.run(resource("invalid_bad_date_format.flightplan"), false));
    }

    @Test
    @DisplayName("[D9] Wrong time format H:MM with no leading zero (file) should fail")
    void invalidBadTimeFormatFile() throws IOException, URISyntaxException {
        // Rule: TIME_LITERAL requires exactly 2 digits before the colon (HH:MM)
        // '8' alone is tokenised as NUMBER, not TIME_LITERAL → parser error
        assertFalse(FlightPlanRunner.run(resource("invalid_bad_time_format.flightplan"), false));
    }

    @Test
    @DisplayName("[D10] Airport code with only 2 letters (file) should fail")
    void invalidBadAirportCodeFile() throws IOException, URISyntaxException {
        // Rule: airportCode accepts IATA_CODE (3 letters) or ICAO_CODE (4 letters) only
        // 'LI' is tokenised as IDENTIFIER → parser error
        assertFalse(FlightPlanRunner.run(resource("invalid_bad_airport_code.flightplan"), false));
    }

    @Test
    @DisplayName("[D11] Unknown token '#' (file) should fail with lexer error")
    void invalidUnknownTokenFile() throws IOException, URISyntaxException {
        // Rule: '#' is not in the Flight DSL alphabet → lexer error
        assertFalse(FlightPlanRunner.run(resource("invalid_unknown_token.flightplan"), false));
    }

    @Test
    @DisplayName("[D12] Missing semicolon after field value (file) should fail")
    void invalidMissingSemicolonFile() throws IOException, URISyntaxException {
        // Rule: every field declaration ends with ';'
        assertFalse(FlightPlanRunner.run(resource("invalid_missing_semicolon.flightplan"), false));
    }

    @Test
    @DisplayName("Missing semicolon should produce parser error")
    void missingSemicolon() throws IOException {
        // Arrange
        String content = """
                flight TP011 : charter {
                    route { origin: LIS; destination: LHR; }
                    leg L1 {
                        departure { airport: LIS; date: 2026-05-10; time: 08:30 }
                        arrival   { airport: LHR; time: 10:45; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359);
                            to:   (51.4775, -0.4614);
                            altitudes: [10000 m];
                            wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("Invalid token '@' should produce lexer error")
    void invalidToken() throws IOException {
        // Arrange
        String content = """
                flight @TP012 : regular {
                    route { origin: LIS; destination: LHR; }
                    leg L1 {
                        departure { airport: LIS; date: 2026-05-10; time: 08:30; }
                        arrival   { airport: LHR; time: 10:45; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359);
                            to:   (51.4775, -0.4614);
                            altitudes: [10000 m];
                            wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("Missing route block should produce parser error")
    void missingRouteBlock() throws IOException {
        // Arrange — route is required at flight level
        String content = """
                flight TP013 : charter {
                    leg L1 {
                        departure { airport: LIS; date: 2026-05-10; time: 08:30; }
                        arrival   { airport: LHR; time: 10:45; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359);
                            to:   (51.4775, -0.4614);
                            altitudes: [10000 m];
                            wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("Missing segment block should produce parser error")
    void missingSegmentBlock() throws IOException {
        // Arrange — at least one segment is required per leg
        String content = """
                flight TP014 : charter {
                    route { origin: LIS; destination: LHR; }
                    leg L1 {
                        departure { airport: LIS; date: 2026-05-10; time: 08:30; }
                        arrival   { airport: LHR; time: 10:45; }
                        fuel      { quantity: 15000 kg; }
                    }
                }
                """;
        // Act + Assert
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("Invalid flight type 'cargo' should produce parser error")
    void invalidFlightType() throws IOException {
        // Arrange — only 'regular' and 'charter' are valid
        String content = """
                flight TP015 : cargo {
                    route { origin: LIS; destination: LHR; }
                    leg L1 {
                        departure { airport: LIS; date: 2026-05-10; time: 08:30; }
                        arrival   { airport: LHR; time: 10:45; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359);
                            to:   (51.4775, -0.4614);
                            altitudes: [10000 m];
                            wind: (270, 15 m/s);
                        }
                    }
                }
                """;
        // Act + Assert
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("Missing closing brace should produce parser error")
    void missingClosingBrace() throws IOException {
        // Arrange
        String content = """
                flight TP016 : charter {
                    route { origin: LIS; destination: LHR; }
                    leg L1 {
                        departure { airport: LIS; date: 2026-05-10; time: 08:30; }
                        arrival   { airport: LHR; time: 10:45; }
                        fuel      { quantity: 15000 kg; }
                        segment S1 {
                            from: (38.7813, -9.1359);
                            to:   (51.4775, -0.4614);
                            altitudes: [10000 m];
                            wind: (270, 15 m/s);
                        }
                    }
                // missing closing brace for flight block
                """;
        // Act + Assert
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("Empty file should produce parser error")
    void emptyFile() throws IOException {
        // Arrange + Act + Assert
        assertFalse(parse(""));
    }
}