package eapli.aisafe.bootstrap;

import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flightplan.application.DslValidator;
import eapli.aisafe.flightplan.application.FlightPlanExporter;
import eapli.aisafe.flightplan.application.FlightPlanToScenarioConverter;
import eapli.aisafe.flightplan.application.ReportParser;
import eapli.aisafe.flightplan.domain.FlightPlan;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.framework.actions.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@SuppressWarnings("squid:S1126")
public class DemoSmokeTester implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoSmokeTester.class);
    private static final LocalDateTime DEP_TIME = LocalDateTime.of(2026, 6, 2, 10, 0);

    private static final String VALID_DSL =
            "flight TP3000 : charter {\n" +
            "    route { origin: LIS; destination: CDG; }\n" +
            "    aircraft: CS-TUB;\n" +
            "    pilot: P12345;\n" +
            "    leg {\n" +
            "        departure { airport: LIS; datetime: 2026-06-02T10:00+01:00; }\n" +
            "        arrival   { airport: CDG; datetime: 2026-06-02T13:30+02:00; }\n" +
            "        fuel      { quantity: 8000 kg; }\n" +
            "        segment {\n" +
            "            from      : (38.7813, -9.1359);\n" +
            "            to        : (49.0097, 2.5479);\n" +
            "            altitudes : [10000 m WIDTH 60 m];\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private static final String SAMPLE_REPORT = """
            ============================================
              AISafe Simulation Report
              Total violations detected: 0
            ============================================

            FLIGHT SUMMARY:
              TP3000: n_viol=0  ever_in_area=yes  completed=yes
              TP5678: n_viol=2  ever_in_area=yes  completed=yes

            ============================================
              RESULT: PASS
            ============================================
            """;

    private int passed;
    private int failed;

    @Override
    public boolean execute() {
        passed = 0;
        failed = 0;

        test("ReportParser - overall PASS", this::testOverallPass);
        test("ReportParser - overall FAIL", this::testOverallFail);
        test("ReportParser - per-flight results", this::testPerFlightResults);
        test("ReportParser - null content", this::testNullContent);
        test("DslValidator - valid DSL", this::testDslValid);
        test("DslValidator - invalid DSL", this::testDslInvalid);
        test("FlightPlanExporter - export", this::testExporter);
        test("FlightPlanToScenarioConverter - valid", this::testConverter);
        test("Flight domain - add flight plan", this::testFlightDomain);

        LOGGER.info("──────────────────────────────────────");
        LOGGER.info("Smoke tests: {} passed, {} failed", passed, failed);
        LOGGER.info("──────────────────────────────────────");

        return failed == 0;
    }

    private void test(final String name, final Runnable testMethod) {
        try {
            testMethod.run();
            passed++;
            LOGGER.info("  [PASS] {}", name);
        } catch (final AssertionError | Exception e) {
            failed++;
            LOGGER.warn("  [FAIL] {}: {}", name, e.getMessage());
        }
    }

    private void testOverallPass() {
        final var result = ReportParser.parse(SAMPLE_REPORT);
        assert result.isPassed() : "Expected PASS";
        assert result.violationCount() == 0 : "Expected 0 violations";
    }

    private void testOverallFail() {
        final var failReport = SAMPLE_REPORT.replace("RESULT: PASS", "RESULT: FAIL");
        final var result = ReportParser.parse(failReport);
        assert !result.isPassed() : "Expected FAIL";
        assert result.violationCount() == 0 : "Expected 0 violations";
    }

    private void testPerFlightResults() {
        final var results = ReportParser.parsePerFlightResults(SAMPLE_REPORT);
        assert results.size() == 2 : "Expected 2 per-flight results, got " + results.size();

        final var tp3000 = results.stream()
                .filter(r -> "TP3000".equals(r.flightId()))
                .findFirst().orElseThrow();
        assert tp3000.isPassed() : "TP3000 should pass (0 violations)";
        assert tp3000.violations() == 0 : "TP3000 violations should be 0";
        assert tp3000.everInArea() : "TP3000 should be ever_in_area";
        assert tp3000.completed() : "TP3000 should be completed";

        final var tp5678 = results.stream()
                .filter(r -> "TP5678".equals(r.flightId()))
                .findFirst().orElseThrow();
        assert !tp5678.isPassed() : "TP5678 should fail (2 violations)";
        assert tp5678.violations() == 2 : "TP5678 violations should be 2";
    }

    private void testNullContent() {
        final var result = ReportParser.parse(null);
        assert !result.isPassed() : "Null report should not pass";
        assert result.perFlightResults().isEmpty() : "Null report should have no per-flight results";
    }

    private void testDslValid() {
        final var validator = new DslValidator();
        final var result = validator.validate(VALID_DSL);
        assert result.isPassed() : "Valid DSL should pass validation";
    }

    private void testDslInvalid() {
        final var validator = new DslValidator();
        final var result = validator.validate("invalid dsl content");
        assert !result.isPassed() : "Invalid DSL should fail validation";
        assert !result.reasons().isEmpty() : "Invalid DSL should have error reasons";
    }

    private void testExporter() {
        final var flight = new Flight(FlightDesignator.valueOf("TP3000"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), VALID_DSL);
        final var exporter = new FlightPlanExporter();
        final var json = exporter.exportForSimulator(fp);
        assert json != null && !json.isBlank() : "Exporter should produce non-blank JSON";
        assert json.contains("TP3000") : "JSON should contain flight ID";
    }

    private void testConverter() {
        final var converter = new FlightPlanToScenarioConverter();
        final var json = converter.convert(VALID_DSL);
        assert json != null && !json.isBlank() : "Converter should produce non-blank JSON";
        assert json.contains("TP3000") : "JSON should contain flight ID TP3000";
        assert json.contains("LIS") : "JSON should contain origin LIS";
        assert json.contains("CDG") : "JSON should contain destination CDG";
    }

    private void testFlightDomain() {
        final var flight = new Flight(FlightDesignator.valueOf("TP9999"), DEP_TIME);
        final var fp1 = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "departure LIS 10:00; arrival OPO 11:00");
        assert fp1 != null : "Should create flight plan";
        assert fp1.status().toString().equals("DRAFT") : "New flight plan should be DRAFT";

        final var fp2 = flight.updateFlightPlan(FlightPlanId.valueOf("FP001"), "departure LIS 12:00; arrival OPO 13:00");
        assert fp2 == fp1 : "updateFlightPlan should return same instance";
        assert fp2.dslContent().contains("12:00") : "Updated DSL should reflect changes";

        final var fp3 = flight.updateFlightPlan(FlightPlanId.valueOf("FP002"), "departure LIS 14:00; arrival OPO 15:00");
        assert fp3 != null : "updateFlightPlan should create new plan if missing";
        assert flight.flightPlans().size() == 2 : "Should have 2 flight plans now";
    }
}
