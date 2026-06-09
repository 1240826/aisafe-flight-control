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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US075 — Parameterized creation tests for the {@link Pilot} aggregate root.
 *
 * <p>Test scenarios are driven by {@code /us075/pilot_creation_test_data.csv}.
 * Each row defines the inputs for a Pilot constructor call and whether the
 * construction should succeed or throw an exception.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>License number must match format: one letter + 4–10 digits.</li>
 *   <li>Company must not be null.</li>
 *   <li>Certified models list must not be null or empty.</li>
 *   <li>Certification date must not be null or in the future.</li>
 *   <li>A new pilot is always created in ACTIVE state.</li>
 * </ul>
 */
class PilotCreationTest {

    @ParameterizedTest(name = "[{0}] {6}")
    @MethodSource("csvTestData")
    void ensurePilotCreationRespectsBusinessRules(
            final String testCaseId,
            final String licenseNumber,
            final String companyIata,
            final Set<AircraftModelCode> certifiedModels,
            final LocalDate certificationDate,
            final boolean expectValid,
            final String failureReason) {

        if (expectValid) {
            final Pilot pilot = assertDoesNotThrow(
                    () -> new Pilot(
                            PilotId.valueOf(licenseNumber),
                            CompanyIATA.valueOf(companyIata),
                            certifiedModels,
                            certificationDate),
                    testCaseId + " — should be created: " + failureReason);

            // Post-conditions for valid creation
            assertTrue(pilot.isActive(),
                    testCaseId + " — new pilot must be ACTIVE");
            assertNotNull(pilot.identity(),
                    testCaseId + " — identity must not be null");
            assertFalse(pilot.certifiedModels().isEmpty(),
                    testCaseId + " — certified models must not be empty");
        } else {
            assertThrows(
                    Exception.class,
                    () -> new Pilot(
                            licenseNumber == null || licenseNumber.isBlank()
                                    ? null : PilotId.valueOf(licenseNumber),
                            companyIata == null || companyIata.isBlank()
                                    ? null : CompanyIATA.valueOf(companyIata),
                            certifiedModels,
                            certificationDate),
                    testCaseId + " — should be rejected: " + failureReason);
        }
    }

    // ── CSV loader ────────────────────────────────────────────────────────────

    static Stream<Arguments> csvTestData() {
        final var rows = new ArrayList<Arguments>();
        try (final var reader = new BufferedReader(new InputStreamReader(
                PilotCreationTest.class.getResourceAsStream("/us075/pilot_creation_test_data.csv"),
                StandardCharsets.UTF_8))) {

            for (final var line : reader.lines().toList()) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("testCaseId")) continue;
                final var p = line.split(",", -1);
                if (p.length < 7) continue;

                final String testCaseId     = p[0].trim();
                final String licenseNumber  = p[1].trim();
                final String companyIata    = p[2].trim();
                final String modelsRaw      = p[3].trim();
                final String dateRaw        = p[4].trim();
                final boolean expectValid   = Boolean.parseBoolean(p[5].trim());
                final String failureReason  = p[6].trim();

                final Set<AircraftModelCode> models = parseModels(modelsRaw);
                final LocalDate certDate = parseDate(dateRaw);

                rows.add(Arguments.of(
                        testCaseId, licenseNumber, companyIata,
                        models, certDate, expectValid, failureReason));
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to load pilot_creation_test_data.csv", e);
        }
        return rows.stream();
    }

    private static Set<AircraftModelCode> parseModels(final String raw) {
        if (raw == null || raw.isBlank()) return new HashSet<>();
        final Set<AircraftModelCode> set = new HashSet<>();
        Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .forEach(code -> set.add(AircraftModelCode.valueOf(code)));
        return set;
    }

    private static LocalDate parseDate(final String raw) {
        if (raw == null || raw.equalsIgnoreCase("BLANK")) return null;
        if (raw.equalsIgnoreCase("FUTURE")) return LocalDate.now().plusDays(1);
        return LocalDate.parse(raw);
    }
}
