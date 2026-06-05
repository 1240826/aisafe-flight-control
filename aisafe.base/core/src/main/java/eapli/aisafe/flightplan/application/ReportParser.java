package eapli.aisafe.flightplan.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
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

    private static final Pattern FLIGHT_SUMMARY_LINE = Pattern.compile(
            "^\\s+(\\w+):\\s+n_viol=(\\d+)\\s+ever_in_area=(\\w+)\\s+completed=(\\w+)",
            Pattern.MULTILINE);

    private ReportParser() {
    }

    public static ReportParseResult parse(final String reportContent) {
        if (reportContent == null || reportContent.isBlank()) {
            return new ReportParseResult(false, 0, reportContent, 0, 0, 0, false, "SIMULATED", List.of());
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

        final var perFlightResults = parsePerFlightResults(reportContent);

        return new ReportParseResult(isPassed, violationCount, reportContent,
                criticalViolations, majorViolations, minorViolations,
                hasUnresolvedConflicts, reportType, perFlightResults);
    }

    public static List<PerFlightResult> parsePerFlightResults(final String reportContent) {
        if (reportContent == null || reportContent.isBlank()) {
            return List.of();
        }

        final List<PerFlightResult> results = new ArrayList<>();
        final Matcher matcher = FLIGHT_SUMMARY_LINE.matcher(reportContent);
        while (matcher.find()) {
            final String flightId = matcher.group(1);
            final int violations = Integer.parseInt(matcher.group(2));
            final boolean everInArea = "yes".equalsIgnoreCase(matcher.group(3));
            final boolean completed = "yes".equalsIgnoreCase(matcher.group(4));
            results.add(new PerFlightResult(flightId, violations, everInArea, completed));
        }
        return Collections.unmodifiableList(results);
    }

    public record ReportParseResult(boolean isPassed, int violationCount, String rawOutput,
                                    int criticalViolations, int majorViolations,
                                    int minorViolations, boolean hasUnresolvedConflicts,
                                    String reportType, List<PerFlightResult> perFlightResults) {
        public ReportParseResult {
            if (rawOutput == null) rawOutput = "";
            if (reportType == null) reportType = "SIMULATED";
            if (perFlightResults == null) perFlightResults = List.of();
        }

        public ReportParseResult(boolean isPassed, int violationCount, String rawOutput,
                                  int criticalViolations, int majorViolations,
                                  int minorViolations, boolean hasUnresolvedConflicts,
                                  String reportType) {
            this(isPassed, violationCount, rawOutput, criticalViolations, majorViolations,
                    minorViolations, hasUnresolvedConflicts, reportType, List.of());
        }
    }

    public record PerFlightResult(String flightId, int violations,
                                   boolean everInArea, boolean completed) {
        public boolean isPassed() {
            return violations == 0 && completed;
        }
    }
}