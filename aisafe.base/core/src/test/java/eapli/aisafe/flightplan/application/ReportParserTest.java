package eapli.aisafe.flightplan.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportParserTest {

    @Test
    void ensurePassReportParsesCorrectly() {
        final var report = generateCReport(0, false, 0, 0, 0, "SIMULATED");
        final var result = ReportParser.parse(report);
        assertTrue(result.isPassed());
        assertEquals(0, result.violationCount());
    }

    @Test
    void ensureFailReportParsesCorrectly() {
        final var report = generateCReport(2, true, 1, 0, 1, "SIMULATED");
        final var result = ReportParser.parse(report);
        assertFalse(result.isPassed());
        assertEquals(2, result.violationCount());
    }

    @Test
    void ensureNullContentReturnsFailed() {
        final var result = ReportParser.parse(null);
        assertFalse(result.isPassed());
        assertEquals(0, result.violationCount());
    }

    @Test
    void ensureBlankContentReturnsFailed() {
        final var result = ReportParser.parse("   ");
        assertFalse(result.isPassed());
        assertEquals(0, result.violationCount());
    }

    @Test
    void ensureRawOutputIsPreserved() {
        final var report = generateCReport(0, false, 0, 0, 0, "SIMULATED");
        final var result = ReportParser.parse(report);
        assertEquals(report, result.rawOutput());
    }

    @Test
    void ensureManyViolationsParsedCorrectly() {
        final var report = generateCReport(5, true, 2, 2, 1, "SIMULATED");
        final var result = ReportParser.parse(report);
        assertEquals(5, result.violationCount());
    }

    @Test
    void ensureViolationBreakdownParsedCorrectly() {
        final var report = generateCReport(5, true, 2, 2, 1, "SIMULATED");
        final var result = ReportParser.parse(report);
        assertEquals(2, result.criticalViolations());
        assertEquals(2, result.majorViolations());
        assertEquals(1, result.minorViolations());
    }

    @Test
    void ensureUnresolvedConflictsDetected() {
        final var report = generateCReport(3, true, 1, 1, 1, "SIMULATED")
                + "  Unresolved conflicts: true\n";
        final var result = ReportParser.parse(report);
        assertTrue(result.hasUnresolvedConflicts());
    }

    @Test
    void ensureNoUnresolvedConflictsByDefault() {
        final var report = generateCReport(2, true, 1, 0, 1, "SIMULATED");
        final var result = ReportParser.parse(report);
        assertFalse(result.hasUnresolvedConflicts());
    }

    @Test
    void ensureReportTypeDefaultsToSimulated() {
        final var report = generateCReport(0, false, 0, 0, 0, null);
        final var result = ReportParser.parse(report);
        assertEquals("SIMULATED", result.reportType());
    }

    @Test
    void ensureSimulatedReportTypeIsParsed() {
        final var report = generateCReport(0, false, 0, 0, 0, "SIMULATED");
        final var result = ReportParser.parse(report);
        assertEquals("SIMULATED", result.reportType());
    }

    @Test
    void ensureExecutedReportTypeIsParsed() {
        final var report = generateCReport(0, false, 0, 0, 0, "EXECUTED");
        final var result = ReportParser.parse(report);
        assertEquals("EXECUTED", result.reportType());
    }

    @Test
    void ensureReportTypeIsCaseInsensitive() {
        final var report = generateCReport(0, false, 0, 0, 0, "executed");
        final var result = ReportParser.parse(report);
        assertEquals("EXECUTED", result.reportType());
    }

    private String generateCReport(final int totalViolations, final boolean withViolationLog,
                                    final int critical, final int major, final int minor,
                                    final String reportType) {
        final var result = (totalViolations == 0) ? "PASS" : "FAIL";
        final var sb = new StringBuilder();
        sb.append("============================================\n");
        sb.append("  AISafe ");
        if (reportType != null) {
            sb.append(reportType.substring(0, 1).toUpperCase())
                    .append(reportType.substring(1).toLowerCase());
        } else {
            sb.append("Simulated");
        }
        sb.append(" Report\n");
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