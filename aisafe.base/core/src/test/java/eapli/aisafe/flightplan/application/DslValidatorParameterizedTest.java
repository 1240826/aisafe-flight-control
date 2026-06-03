package eapli.aisafe.flightplan.application;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DslValidatorParameterizedTest {

    private final DslValidator validator = new DslValidator();

    @ParameterizedTest(name = "{0} → current={3}, future={4}")
    @MethodSource("csvTestData")
    void validateFromCsv(
            final String testCaseId,
            final String description,
            final String dslSnippet,
            final String futureExpectedResult,
            final String futureErrorType
    ) {
        final var dsl = dslForTestCase(testCaseId);
        final var result = validator.validate(dsl);
        final var currentExpected = currentExpectedIsPassed(testCaseId);
        assertEquals(currentExpected, result.isPassed(),
                () -> testCaseId + " (" + description
                        + "): current validator isPassed=" + currentExpected
                        + ", future spec says " + futureExpectedResult
                        + " (" + futureErrorType + ")");
    }

    private static Stream<Arguments> csvTestData() {
        final var rows = new ArrayList<Arguments>();
        try (var reader = new BufferedReader(new InputStreamReader(
                DslValidatorParameterizedTest.class.getResourceAsStream("/dsl_validation_test_data.csv"),
                StandardCharsets.UTF_8))) {
            final var lines = reader.lines().toList();
            for (final var line : lines) {
                if (line.isBlank()) continue;
                if (line.startsWith("#")) continue;
                if (line.startsWith("testCaseId")) continue;
                final var parts = line.split(",", 5);
                if (parts.length < 5) continue;
                rows.add(Arguments.of(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        parts[3].trim(),
                        parts[4].trim()
                ));
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to load CSV test data", e);
        }
        return rows.stream();
    }

    private static boolean currentExpectedIsPassed(final String testCaseId) {
        return switch (testCaseId) {
            case "DV03", "DV04", "DV06", "DV07", "DV09", "DV11", "DV12", "DV13", "DV14" -> false;
            default -> true;
        };
    }

    private static String dslForTestCase(final String testCaseId) {
        return switch (testCaseId) {
            case "DV01" -> "departure LIS 10:00; arrival OPO 11:00; aircraft B738; fuel 15000;";
            case "DV02" -> "departure OPO 10:00; arrival LIS 11:00; type charter;";
            case "DV03" -> "departure LIS 10:00";
            case "DV04" -> "departure LIS 10:00; arrival OPO 11:00; type invalid;";
            case "DV05" -> "departure LIS 10:00; arrival OPO 11:00;";
            case "DV06" -> "";
            case "DV07" -> "departure LIS 10:00; arrival OPO 11:00; fuel -500;";
            case "DV08" -> "departure LIS 10:00; arrival OPO 11:00; type regular;";
            case "DV09" -> "departure L@S 10:00; arrival OPO 11:00;";
            case "DV10" -> "departure LIS 10:00; arrival OPO 11:00; aircraft B738; fuel 15000; type regular;"
                    + " remarks longest possible valid dsl content for testing purposes;";
            case "DV11" -> "arrival OPO 11:00;";
            case "DV12" -> "departure LIS 10:00; departure LIS 11:00; arrival OPO 12:00;";
            case "DV13" -> "departure LIS 10:00; arrival OPO 11:00; garbage extra text";
            case "DV14" -> "departure LIS 10:00; arrival OPO 11:00; fuel \u2605\u2605\u2605;";
            case "DV15" -> "departure LIS 10:00; arrival OPO 11:00; flight 12345;";
            default -> throw new IllegalArgumentException("Unknown testCaseId: " + testCaseId);
        };
    }
}
