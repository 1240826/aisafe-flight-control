package eapli.aisafe.flightplan.application;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PilotCertificationDataValidationTest {

    @ParameterizedTest(name = "{0} → pilot {2} on {3} ({7})")
    @MethodSource("csvTestData")
    void ensureCertificationRulesMatchExpectedStatus(
            final String testCaseId,
            final String pilotLicense,
            final String pilotName,
            final String aircraftType,
            final LocalDate certificationDate,
            final LocalDate flightDate,
            final String expectedStatus,
            final String certificationType
    ) {
        assertNotNull(pilotLicense, testCaseId + ": pilotLicense must not be null");
        assertNotNull(pilotName, testCaseId + ": pilotName must not be null");
        assertNotNull(aircraftType, testCaseId + ": aircraftType must not be null");
        assertNotNull(certificationDate, testCaseId + ": certificationDate must not be null");
        assertNotNull(flightDate, testCaseId + ": flightDate must not be null");
        assertNotNull(expectedStatus, testCaseId + ": expectedStatus must not be null");
        assertNotNull(certificationType, testCaseId + ": certificationType must not be null");

        final String actualStatus = checkCertification(certificationType, certificationDate, flightDate)
                ? "CERTIFIED" : "NOT_CERTIFIED";

        assertEquals(expectedStatus, actualStatus,
                () -> testCaseId + " (" + certificationType
                        + "): certification status mismatch");
    }

    private static boolean checkCertification(
            final String certificationType,
            final LocalDate certificationDate,
            final LocalDate flightDate
    ) {
        if ("EXPIRED".equalsIgnoreCase(certificationType)) {
            return false;
        }

        final int validityMonths = "INITIAL".equalsIgnoreCase(certificationType) ? 24 : 12;

        return !certificationDate.plusMonths(validityMonths).isBefore(flightDate);
    }

    private static Stream<Arguments> csvTestData() {
        final var rows = new ArrayList<Arguments>();
        try (var reader = new BufferedReader(new InputStreamReader(
                PilotCertificationDataValidationTest.class.getResourceAsStream(
                        "/pilot_certification_test_data.csv"),
                StandardCharsets.UTF_8))) {
            final var lines = reader.lines().toList();
            for (final var line : lines) {
                if (line.isBlank()) continue;
                if (line.startsWith("#")) continue;
                if (line.startsWith("testCaseId")) continue;
                final var parts = line.split(",");
                if (parts.length < 8) continue;
                rows.add(Arguments.of(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        parts[3].trim(),
                        LocalDate.parse(parts[4].trim()),
                        LocalDate.parse(parts[5].trim()),
                        parts[6].trim(),
                        parts[7].trim()
                ));
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to load CSV test data", e);
        }
        return rows.stream();
    }
}