package aisafe.lprog.semantic;

import aisafe.lprog.dsl.FlightPlanRunner;
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
 * Semantic validation tests — one test per semantic rule (R2–R11).
 *
 * Each test loads a .flightplan file that is syntactically valid
 * but violates exactly one semantic rule, and verifies that
 * FlightPlanRunner.run() returns false.
 *
 * The valid file tests verify that semantically correct plans still pass.
 *
 * All charter flights use: datetime: YYYY-MM-DDTHH:MM+TZ (ISO 8601 with timezone).
 * All regular flights use: day: DayOfWeek; time: HH:MM; (departure) / time: HH:MM; (arrival).
 */
class SemanticValidationTest {

    private Path resource(String name) throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource("examples/" + name),
                "Resource not found: " + name).toURI());
    }

    private boolean parse(String content) throws IOException {
        Path tmp = Files.createTempFile("sem_test_", ".flightplan");
        try {
            Files.writeString(tmp, content);
            return FlightPlanRunner.run(tmp, false);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // VALID — semantic checks must NOT fire on correct plans

    @Test
    @DisplayName("Valid direct flight (disk) passes all semantic rules")
    void validDirectFlightPassesSemantic() throws IOException, URISyntaxException {
        assertTrue(FlightPlanRunner.run(resource("valid_direct_flight.flightplan"), false));
    }

    @Test
    @DisplayName("Valid multi-leg charter (disk) passes all semantic rules")
    void validMultiLegPassesSemantic() throws IOException, URISyntaxException {
        assertTrue(FlightPlanRunner.run(resource("valid_multi_leg.flightplan"), false));
    }


    // R1 — one flight per file (enforced by grammar: flightFile : flightDecl EOF)

    @Test
    @DisplayName("[R1] Two flight declarations in same file should fail (parser error at second 'flight')")
    void r1DuplicateFlightId() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_sem_duplicate_id.flightplan"), false));
    }

    @Test
    @DisplayName("[R1] Same identifier in different runs is allowed (listener is per-instance)")
    void r1SameIdInDifferentRunsIsAllowed() throws IOException {
        // Two separate runs — each creates a new listener; no shared state between runs
        String content = """
                flight TP200 : charter {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg {
                        departure { airport: LIS; datetime: 2026-05-20T08:00+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-20T10:00+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [10000 m WIDTH 60 m];
                        }
                    }
                }
                """;
        assertTrue(parse(content));
        assertTrue(parse(content)); // second run — same id is OK in a separate file
    }


    // R2 — fuel quantity strictly positive

    @Test
    @DisplayName("[R2] Zero fuel quantity should fail")
    void r2ZeroFuel() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_sem_zero_fuel.flightplan"), false));
    }

    @Test
    @DisplayName("[R2] Negative fuel quantity should fail")
    void r2NegativeFuel() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_sem_negative_fuel.flightplan"), false));
    }

    // R3 — consecutive leg airport connection

    @Test
    @DisplayName("[R3] Leg arrival airport != next leg departure airport should fail")
    void r3LegAirportGap() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_sem_leg_airport_gap.flightplan"), false));
    }

    // R4 — leg time ordering (timezone-aware for charter)

    @Test
    @DisplayName("[R4] Leg arrival after next leg departure (UTC comparison) should fail")
    void r4LegTimeOrder() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_sem_leg_time_order.flightplan"), false));
    }

    // R5 — route origin == first leg departure

    @Test
    @DisplayName("[R5] Route origin mismatch with first leg departure should fail")
    void r5RouteOriginMismatch() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_sem_route_origin_mismatch.flightplan"), false));
    }

    // R6 — route destination == last leg arrival

    @Test
    @DisplayName("[R6] Route destination mismatch with last leg arrival should fail")
    void r6RouteDestMismatch() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_sem_route_dest_mismatch.flightplan"), false));
    }

    // R7 — no airport visited twice

    @Test
    @DisplayName("[R7] Round-trip flight revisiting departure airport should fail")
    void r7AirportRevisited() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_sem_airport_revisited.flightplan"), false));
    }

    // R8 — segment from != to

    @Test
    @DisplayName("[R8] Segment with identical from and to coordinates should fail")
    void r8SameCoordinates() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_sem_same_coords.flightplan"), false));
    }

    @Test
    @DisplayName("[R8] Segment with different coordinates should pass")
    void r8DifferentCoordinatesPass() throws IOException {
        // Verifies the rule does not fire on valid input
        String content = """
                flight TP201 : charter {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg {
                        departure { airport: LIS; datetime: 2026-05-20T08:00+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-20T10:00+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [10000 m WIDTH 60 m];
                        }
                    }
                }
                """;
        assertTrue(parse(content));
    }

    // R9 — altitude must be positive; width must be positive

    @Test
    @DisplayName("[R9] Zero altitude should fail")
    void r9ZeroAltitude() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_sem_zero_altitude.flightplan"), false));
    }

    @Test
    @DisplayName("[R9] Negative corridor width should fail (inline)")
    void r9NegativeWidth() throws IOException {
        String content = """
                flight TP202 : charter {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg {
                        departure { airport: LIS; datetime: 2026-05-20T08:00+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-20T10:00+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [10000 m WIDTH -50 m];
                        }
                    }
                }
                """;
        assertFalse(parse(content));
    }

    // R10 — valid ISO 8601 datetime with timezone; valid time values

    @Test
    @DisplayName("[R10] Invalid calendar date Feb 30 in timestamp should fail")
    void r10InvalidCalendarDate() throws IOException {
        // OffsetDateTime.parse("2026-02-30T08:00+01:00") throws DateTimeParseException
        String content = """
                flight TP203 : charter {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg {
                        departure { airport: LIS; datetime: 2026-02-30T08:00+01:00; }
                        arrival   { airport: LHR; datetime: 2026-02-30T10:00+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [10000 m WIDTH 60 m];
                        }
                    }
                }
                """;
        assertFalse(parse(content));
    }

    @Test
    @DisplayName("[R10] Invalid hour 25 in timestamp should fail")
    void r10InvalidTime() throws IOException {
        // TIMESTAMP_LITERAL pattern matches '25' (2 digits) but OffsetDateTime.parse rejects hour > 23
        String content = """
                flight TP204 : charter {
                    route { origin: LIS; destination: LHR; }
                    aircraft : CS-TUB;
                    pilot    : P12345;
                    leg {
                        departure { airport: LIS; datetime: 2026-05-20T25:00+01:00; }
                        arrival   { airport: LHR; datetime: 2026-05-20T27:00+01:00; }
                        fuel      { quantity: 15000 kg; }
                        segment {
                            from: (38.7813, -9.1359); to: (51.4775, -0.4614);
                            altitudes: [10000 m WIDTH 60 m];
                        }
                    }
                }
                """;
        assertFalse(parse(content));
    }

    // R11 — schedule type must match flight type

    @Test
    @DisplayName("[R11] Regular flight using datetime: instead of day: should fail")
    void r11RegularUsesDatetime() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_sem_r11_regular_uses_date.flightplan"), false));
    }

    @Test
    @DisplayName("[R11] Charter flight using day: instead of datetime: should fail")
    void r11CharterUsesDay() throws IOException, URISyntaxException {
        assertFalse(FlightPlanRunner.run(resource("invalid_sem_r11_charter_uses_day.flightplan"), false));
    }

    @Test
    @DisplayName("[R11] Regular flight with day: passes semantic check")
    void r11RegularWithDayPasses() throws IOException, URISyntaxException {
        assertTrue(FlightPlanRunner.run(resource("valid_regular_multi_leg.flightplan"), false));
    }
}
