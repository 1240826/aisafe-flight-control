package eapli.aisafe.pilot.domain;

import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US077 — Parameterized deactivation tests for the {@link Pilot} aggregate root.
 *
 * <p>Test scenarios are driven by {@code /us077/pilot_deactivation_test_data.csv}.
 * Each row defines how many times {@code deactivate()} is called and whether the
 * final call should succeed or throw.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>An active pilot can be deactivated exactly once.</li>
 *   <li>Deactivating an already inactive pilot throws {@link IllegalStateException}.</li>
 *   <li>Deactivation is irreversible — the pilot stays inactive.</li>
 * </ul>
 */
class PilotDeactivationTest {

    private static final CompanyIATA        COMPANY   = CompanyIATA.valueOf("TP");
    private static final Set<AircraftModelCode> MODELS = Set.of(AircraftModelCode.valueOf("B738"));
    private static final LocalDate          CERT_DATE = LocalDate.of(2022, 3, 10);

    @ParameterizedTest(name = "[{0}] deactivate {2}x → expectSuccess={3} — {5}")
    @MethodSource("csvTestData")
    void ensureDeactivationRespectsBusinessRules(
            final String testCaseId,
            final String licenseNumber,
            final int deactivateCount,
            final boolean expectSuccess,
            final String expectedActiveState,
            final String failureReason) {

        final Pilot pilot = new Pilot(
                PilotId.valueOf(licenseNumber), COMPANY, MODELS, CERT_DATE);

        // Apply deactivate() calls up to (deactivateCount - 1) without asserting
        for (int i = 1; i < deactivateCount; i++) {
            try { pilot.deactivate(); } catch (final Exception ignored) { }
        }

        // Assert on the final call
        if (expectSuccess) {
            assertDoesNotThrow(pilot::deactivate,
                    testCaseId + " — deactivation should succeed: " + failureReason);
            assertFalse(pilot.isActive(),
                    testCaseId + " — pilot must be INACTIVE after deactivation");
        } else {
            assertThrows(Exception.class, pilot::deactivate,
                    testCaseId + " — deactivation should throw: " + failureReason);
        }

        // Final state must match expectedActiveState regardless of outcome
        final boolean expectedActive = "ACTIVE".equalsIgnoreCase(expectedActiveState);
        assertEquals(expectedActive, pilot.isActive(),
                testCaseId + " — final active state must be " + expectedActiveState);
    }

    // ── CSV loader ────────────────────────────────────────────────────────────

    static Stream<Arguments> csvTestData() {
        final var rows = new ArrayList<Arguments>();
        try (final var reader = new BufferedReader(new InputStreamReader(
                PilotDeactivationTest.class.getResourceAsStream(
                        "/us077/pilot_deactivation_test_data.csv"),
                StandardCharsets.UTF_8))) {

            for (final var line : reader.lines().toList()) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("testCaseId")) continue;
                final var p = line.split(",", -1);
                if (p.length < 6) continue;

                rows.add(Arguments.of(
                        p[0].trim(),
                        p[1].trim(),
                        Integer.parseInt(p[2].trim()),
                        Boolean.parseBoolean(p[3].trim()),
                        p[4].trim(),
                        p[5].trim()));
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to load pilot_deactivation_test_data.csv", e);
        }
        return rows.stream();
    }
}
