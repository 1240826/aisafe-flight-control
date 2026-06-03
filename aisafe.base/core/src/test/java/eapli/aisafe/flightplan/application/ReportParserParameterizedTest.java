package eapli.aisafe.flightplan.application;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportParserParameterizedTest {

    @ParameterizedTest(name = "{0}: totalViolations={2}, type={1}")
    @MethodSource("csvTestData")
    void ensureReportParsesCorrectlyFromCsv(
            final String testCaseId,
            final String reportType,
            final int totalViolations,
            final int criticalViolations,
            final int majorViolations,
            final int minorViolations,
            final boolean hasUnresolvedConflicts,
            final int expectedCriticalViolations,
            final int expectedMajorViolations,
            final int expectedMinorViolations,
            final boolean expectedUnresolvedConflicts
    ) {
        final var report = generateCReport(totalViolations, totalViolations > 0,
                criticalViolations, majorViolations, minorViolations,
                hasUnresolvedConflicts, reportType);
        final var result = ReportParser.parse(report);
        assertEquals(totalViolations, result.violationCount(),
                () -> testCaseId + ": violationCount mismatch");
        assertEquals(totalViolations == 0, result.isPassed(),
                () -> testCaseId + ": isPassed mismatch");
        assertEquals(expectedCriticalViolations, result.criticalViolations(),
                () -> testCaseId + ": criticalViolations mismatch");
        assertEquals(expectedMajorViolations, result.majorViolations(),
                () -> testCaseId + ": majorViolations mismatch");
        assertEquals(expectedMinorViolations, result.minorViolations(),
                () -> testCaseId + ": minorViolations mismatch");
        assertEquals(expectedUnresolvedConflicts, result.hasUnresolvedConflicts(),
                () -> testCaseId + ": hasUnresolvedConflicts mismatch");
        assertEquals(reportType.toUpperCase(), result.reportType(),
                () -> testCaseId + ": reportType mismatch");
    }

    private static Stream<Arguments> csvTestData() {
        final var rows = new ArrayList<Arguments>();
        try (var reader = new BufferedReader(new InputStreamReader(
                ReportParserParameterizedTest.class.getResourceAsStream("/simulation_report_test_data.csv"),
                StandardCharsets.UTF_8))) {
            final var lines = reader.lines().toList();
            for (final var line : lines) {
                if (line.isBlank()) continue;
                if (line.startsWith("#")) continue;
                if (line.startsWith("testCaseId")) continue;
                final var parts = line.split(",");
                if (parts.length < 11) continue;
                rows.add(Arguments.of(
                        parts[0].trim(),
                        parts[1].trim(),
                        Integer.parseInt(parts[2].trim()),
                        Integer.parseInt(parts[3].trim()),
                        Integer.parseInt(parts[4].trim()),
                        Integer.parseInt(parts[5].trim()),
                        Boolean.parseBoolean(parts[6].trim()),
                        Integer.parseInt(parts[7].trim()),
                        Integer.parseInt(parts[8].trim()),
                        Integer.parseInt(parts[9].trim()),
                        Boolean.parseBoolean(parts[10].trim())
                ));
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to load CSV test data", e);
        }
        return rows.stream();
    }

    private String generateCReport(final int totalViolations, final boolean withViolationLog,
                                    final int critical, final int major, final int minor,
                                    final boolean unresolved, final String reportType) {
        final var result = (totalViolations == 0) ? "PASS" : "FAIL";
        final var sb = new StringBuilder();
        sb.append("============================================\n");
        sb.append("  AISafe ");
        final String typeLabel = reportType != null
                ? reportType.substring(0, 1).toUpperCase() + reportType.substring(1).toLowerCase()
                : "Simulated";
        sb.append(typeLabel).append(" Report\n");
        sb.append("  Generated: Mon Jun  1 12:00:00 2026\n");
        if (reportType != null) {
            sb.append("  Report type: ").append(reportType.toUpperCase()).append("\n");
        }
        sb.append("  Total steps: 7200  (7200 seconds simulated)\n");
        sb.append("  Flights: 1\n");
        sb.append("  Total violations detected: ").append(totalViolations).append("\n");
        sb.append("  Critical violations: ").append(critical).append("\n");
        sb.append("  Major violations: ").append(major).append("\n");
        sb.append("  Minor violations: ").append(minor).append("\n");
        sb.append("  Unresolved conflicts: ").append(unresolved).append("\n");
        sb.append("============================================\n\n");
        sb.append("FLIGHT SUMMARY:\n");
        sb.append("  TP0123: n_viol=").append(totalViolations).append("  ever_in_area=yes  completed=yes\n");
        if (withViolationLog && totalViolations > 0) {
            sb.append("\nVIOLATION LOG:\n");
            for (int i = 1; i <= totalViolations; i++) {
                sb.append("  #").append(i).append(" step=").append(i * 10)
                        .append("  TP0123 <-> TP0123  h_dist=5000m  v_dist=200m")
                        .append("  pos_a=(40.0000,-8.9000,5000)  pos_b=(39.5000,-9.0000,3000)\n");
            }
        }
        sb.append("\n============================================\n");
        sb.append("  RESULT: ").append(result).append("\n");
        sb.append("============================================\n");
        return sb.toString();
    }
}