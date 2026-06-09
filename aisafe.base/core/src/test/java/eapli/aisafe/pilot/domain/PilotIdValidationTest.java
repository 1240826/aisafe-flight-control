package eapli.aisafe.pilot.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US075 — Parameterized validation tests for {@link PilotId}.
 *
 * <p>Test scenarios are driven by {@code /us075/pilot_id_test_data.csv}.
 * Each row defines a license input, whether it should be accepted or rejected,
 * and a human-readable description of the scenario.
 *
 * <p>Format rule enforced by {@link PilotId}: one uppercase letter followed by
 * 4 to 10 digits (e.g. {@code P12345}). Lowercase letters are normalised to uppercase.
 */
class PilotIdValidationTest {

    @ParameterizedTest(name = "[{0}] {3} → expectValid={2}")
    @MethodSource("csvTestData")
    void ensureLicenseFormatIsValidatedCorrectly(
            final String testCaseId,
            final String licenseInput,
            final boolean expectValid,
            final String description) {

        if (expectValid) {
            assertDoesNotThrow(
                    () -> PilotId.valueOf(licenseInput),
                    testCaseId + " — should be accepted: " + description);
        } else {
            assertThrows(
                    Exception.class,
                    () -> PilotId.valueOf(licenseInput),
                    testCaseId + " — should be rejected: " + description);
        }
    }

    @ParameterizedTest(name = "[{0}] valid license normalised to uppercase")
    @MethodSource("validLicenses")
    void ensureValidLicenseStoredInUpperCase(
            final String testCaseId,
            final String licenseInput,
            final boolean expectValid,
            final String description) {

        if (!expectValid) return; // only test valid ones here
        final PilotId id = PilotId.valueOf(licenseInput);
        assertEquals(licenseInput.trim().toUpperCase(), id.licenseNumber(),
                testCaseId + " — licenseNumber must be uppercase");
    }

    @ParameterizedTest(name = "[{0}] equal PilotIds have same hashCode")
    @MethodSource("validLicenses")
    void ensureEqualPilotIdsHaveSameHashCode(
            final String testCaseId,
            final String licenseInput,
            final boolean expectValid,
            final String description) {

        if (!expectValid) return;
        final PilotId a = PilotId.valueOf(licenseInput);
        final PilotId b = PilotId.valueOf(licenseInput);
        assertEquals(a.hashCode(), b.hashCode(),
                testCaseId + " — equal PilotIds must have same hashCode");
    }

    // ── CSV loaders ───────────────────────────────────────────────────────────

    static Stream<Arguments> csvTestData() {
        return loadCsv();
    }

    static Stream<Arguments> validLicenses() {
        return loadCsv().filter(args -> (boolean) args.get()[2]);
    }

    private static Stream<Arguments> loadCsv() {
        final var rows = new ArrayList<Arguments>();
        try (final var reader = new BufferedReader(new InputStreamReader(
                PilotIdValidationTest.class.getResourceAsStream("/us075/pilot_id_test_data.csv"),
                StandardCharsets.UTF_8))) {

            for (final var line : reader.lines().toList()) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("testCaseId")) continue;
                final var p = line.split(",", -1);
                if (p.length < 4) continue;
                rows.add(Arguments.of(
                        p[0].trim(),
                        p[1].trim(),
                        Boolean.parseBoolean(p[2].trim()),
                        p[3].trim()));
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to load pilot_id_test_data.csv", e);
        }
        return rows.stream();
    }
}
