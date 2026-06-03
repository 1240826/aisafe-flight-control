package eapli.aisafe.flightplan.application;

import java.util.regex.Pattern;

public class ReportParser {

    private static final Pattern RESULT_PATTERN = Pattern.compile(
            "RESULT:\\s*(PASS|FAIL)", Pattern.CASE_INSENSITIVE);

    private static final Pattern VIOLATIONS_PATTERN = Pattern.compile(
            "Total violations detected:\\s*(\\d+)");

    private static final Pattern CRITICAL_PATTERN = Pattern.compile(
            "Critical violations:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern MAJOR_PATTERN = Pattern.compile(
            "Major violations:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern MINOR_PATTERN = Pattern.compile(
            "Minor violations:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern UNRESOLVED_PATTERN = Pattern.compile(
            "Unresolved conflicts:\\s*(true|false)", Pattern.CASE_INSENSITIVE);

    private static final Pattern REPORT_TYPE_PATTERN = Pattern.compile(
            "Report type:\\s*(EXECUTED|SIMULATED)", Pattern.CASE_INSENSITIVE);

    private ReportParser() {
    }

    public static ReportParseResult parse(final String reportContent) {
        if (reportContent == null || reportContent.isBlank()) {
            return new ReportParseResult(false, 0, reportContent, 0, 0, 0, false, "SIMULATED");
        }

        final var resultMatcher = RESULT_PATTERN.matcher(reportContent);
        final boolean isPassed = resultMatcher.find()
                && "PASS".equalsIgnoreCase(resultMatcher.group(1));

        final var violationsMatcher = VIOLATIONS_PATTERN.matcher(reportContent);
        final int violationCount = violationsMatcher.find()
                ? Integer.parseInt(violationsMatcher.group(1))
                : 0;

        final var criticalMatcher = CRITICAL_PATTERN.matcher(reportContent);
        final int criticalViolations = criticalMatcher.find()
                ? Integer.parseInt(criticalMatcher.group(1))
                : 0;

        final var majorMatcher = MAJOR_PATTERN.matcher(reportContent);
        final int majorViolations = majorMatcher.find()
                ? Integer.parseInt(majorMatcher.group(1))
                : 0;

        final var minorMatcher = MINOR_PATTERN.matcher(reportContent);
        final int minorViolations = minorMatcher.find()
                ? Integer.parseInt(minorMatcher.group(1))
                : 0;

        final var unresolvedMatcher = UNRESOLVED_PATTERN.matcher(reportContent);
        final boolean hasUnresolvedConflicts = unresolvedMatcher.find()
                && "true".equalsIgnoreCase(unresolvedMatcher.group(1));

        final var reportTypeMatcher = REPORT_TYPE_PATTERN.matcher(reportContent);
        final String reportType = reportTypeMatcher.find()
                ? reportTypeMatcher.group(1).toUpperCase()
                : "SIMULATED";

        return new ReportParseResult(isPassed, violationCount, reportContent,
                criticalViolations, majorViolations, minorViolations,
                hasUnresolvedConflicts, reportType);
    }

    public record ReportParseResult(boolean isPassed, int violationCount, String rawOutput,
                                    int criticalViolations, int majorViolations,
                                    int minorViolations, boolean hasUnresolvedConflicts,
                                    String reportType) {
        public ReportParseResult {
            if (rawOutput == null) rawOutput = "";
            if (reportType == null) reportType = "SIMULATED";
        }
    }
}