package eapli.aisafe.flightplan.application;

import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.flightplan.domain.FlightPlan;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UseCaseController
public class TestFlightPlanController {

    private static final Pattern DSL_DEPARTURE_PATTERN = Pattern.compile(
            "departure\\s+\\w+\\s+(\\d{2}):(\\d{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern ISO_TIME_PATTERN = Pattern.compile(
            "datetime\\s*:\\s*\\d{4}-\\d{2}-\\d{2}T(\\d{2}):(\\d{2})", Pattern.CASE_INSENSITIVE);

    private final AuthorizationService authz;
    private final FlightRepository flightRepo;
    private final FlightPlanExporter exporter;
    private final SimulationRunner runner;
    private final DslValidator dslValidator;

    public TestFlightPlanController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().flights(),
                new FlightPlanExporter(),
                createRunner(),
                new DslValidator());
    }

    TestFlightPlanController(final AuthorizationService authz,
                              final FlightRepository flightRepo,
                              final FlightPlanExporter exporter,
                              final SimulationRunner runner,
                              final DslValidator dslValidator) {
        this.authz = authz;
        this.flightRepo = flightRepo;
        this.exporter = exporter;
        this.runner = runner;
        this.dslValidator = dslValidator;
    }

    private static SimulationRunner createRunner() {
        final var hostProp = System.getProperty("aisafe.simulator.host");
        final int timeout = Integer.getInteger("aisafe.simulator.timeout", 120);
        if (hostProp != null && !hostProp.isEmpty()) {
            final int port = Integer.getInteger("aisafe.simulator.port", 9999);
            return new SocketSimulationRunner(hostProp, port, timeout);
        }
        final String executable = System.getProperty("aisafe.simulator.executable", "aisafe-simulator");
        return new ProcessBuilderSimulationRunner(executable, timeout);
    }

    public record TestResult(boolean passed, String message, String reportContent) {
    }

    public record FlightPlanEntry(Flight flight, FlightPlan flightPlan) {
    }

    public record ScenarioResult(boolean passed, String message, String reportContent,
                                  java.util.List<FlightPlanEntry> results) {
    }

    public ScenarioResult testScenario(final java.util.List<FlightPlanEntry> entries) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);

        final java.util.List<FlightPlanEntry> tested = new ArrayList<>();
        final java.util.List<String> errors = new ArrayList<>();

        for (final var entry : entries) {
            final var flight = entry.flight();
            final var flightPlan = entry.flightPlan();

            final var st = flightPlan.status();
            if (st != eapli.aisafe.flightplan.domain.FlightPlanStatus.DRAFT
                    && st != eapli.aisafe.flightplan.domain.FlightPlanStatus.TEST_PASSED
                    && st != eapli.aisafe.flightplan.domain.FlightPlanStatus.TEST_FAILED) {
                errors.add("Skipped " + flightPlan.identity()
                        + " (status: " + st + ")");
                continue;
            }
            if (st != eapli.aisafe.flightplan.domain.FlightPlanStatus.DRAFT) {
                flightPlan.resetToDraft();
            }

            final var departureCheck = checkDepartureTime(flight, flightPlan.dslContent());
            if (!departureCheck.isPassed()) {
                errors.add("Skipped " + flight.identity() + ": " + departureCheck.message());
                continue;
            }

            final var dslValidation = dslValidator.validate(flightPlan.dslContent());
            if (!dslValidation.isPassed()) {
                errors.add("Skipped " + flight.identity()
                        + ": DSL validation: " + String.join("; ", dslValidation.reasons()));
                continue;
            }

            flightPlan.markAsInTest();
            flightRepo.save(flight);
            tested.add(entry);
        }

        if (tested.isEmpty()) {
            return new ScenarioResult(false,
                    "No valid flight plans to test: " + String.join("; ", errors),
                    null, List.of());
        }

        // Build JSON array: each exportForSimulator returns [{singleFlight}],
        // strip the outer brackets and join with commas.
        final var sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < tested.size(); i++) {
            if (i > 0) sb.append(",\n");
            final var entryJson = exporter.exportForSimulator(tested.get(i).flightPlan()).strip();
            // Remove leading [ and trailing ]
            if (entryJson.startsWith("[")) {
                sb.append(entryJson, 1, entryJson.lastIndexOf(']')).append('\n');
            } else {
                sb.append(entryJson).append('\n');
            }
        }
        sb.append("]\n");
        final String json = sb.toString();

        final String reportContent;
        try {
            reportContent = runner.run(json);
        } catch (final SimulationRunnerException e) {
            for (final var entry : tested) {
                entry.flightPlan().resetToDraft();
                flightRepo.save(entry.flight());
            }
            return new ScenarioResult(false,
                    "Simulation execution failed: " + e.getMessage(), null, tested);
        }

        final var parsed = ReportParser.parse(reportContent);
        final var perFlight = parsed.perFlightResults();

        int passedCount = 0;
        int failedCount = 0;
        for (final var entry : tested) {
            final String flightId = entry.flight().identity().toString();
            final boolean flightPassed;

            if (!perFlight.isEmpty()) {
                flightPassed = perFlight.stream()
                        .filter(r -> r.flightId().equals(flightId))
                        .findFirst()
                        .map(ReportParser.PerFlightResult::isPassed)
                        .orElse(parsed.isPassed());
            } else {
                flightPassed = parsed.isPassed();
            }

            entry.flightPlan().recordTestResult(flightPassed, null, reportContent);
            flightRepo.save(entry.flight());

            if (flightPassed) passedCount++; else failedCount++;
        }

        final var skippedMsg = errors.isEmpty() ? ""
                : " [" + String.join("; ", errors) + "]";
        final boolean allPassed = failedCount == 0;
        final var msg = allPassed
                ? "Scenario PASSED - " + tested.size() + " flight(s) tested"
                : "Scenario FAILED - " + tested.size() + " flight(s) tested ("
                + passedCount + " passed, " + failedCount + " failed)";
        return new ScenarioResult(allPassed, msg + skippedMsg, reportContent, tested);
    }

    public TestResult testFlightPlan(final String flightDesignatorStr,
                                      final String flightPlanIdStr) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);

        final var flightDesignator = eapli.aisafe.flight.domain.FlightDesignator.valueOf(
                flightDesignatorStr);
        final var flight = flightRepo
                .ofIdentity(flightDesignator)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight not found: " + flightDesignator));

        final var flightPlanId = FlightPlanId.valueOf(flightPlanIdStr);
        final var flightPlan = flight
                .flightPlan(flightPlanId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight plan " + flightPlanId + " not found in flight "
                                + flightDesignator));

        return executeTest(flight, flightPlan);
    }

    public TestResult testFlightPlan(final String flightPlanIdStr) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);

        final var flightPlanId = FlightPlanId.valueOf(flightPlanIdStr);
        final var flight = flightRepo
                .findByFlightPlanId(flightPlanId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight plan not found: " + flightPlanId));

        final var flightPlan = flight
                .flightPlan(flightPlanId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight plan " + flightPlanId + " not found in flight"));

        return executeTest(flight, flightPlan);
    }

    private TestResult executeTest(final Flight flight, final FlightPlan flightPlan) {
        if (flightPlan.status() != eapli.aisafe.flightplan.domain.FlightPlanStatus.DRAFT) {
            return new TestResult(false,
                    "Flight plan is not in DRAFT status (current: " + flightPlan.status() + ")",
                    null);
        }

        final var departureCheck = checkDepartureTime(flight, flightPlan.dslContent());
        if (!departureCheck.isPassed()) {
            flightPlan.markAsInTest();
            flightPlan.recordTestResult(false, null,
                    departureCheck.message());
            flightRepo.save(flight);
            return new TestResult(false, departureCheck.message(), null);
        }

        final var dslValidation = dslValidator.validate(flightPlan.dslContent());
        if (!dslValidation.isPassed()) {
            flightPlan.markAsInTest();
            flightPlan.recordTestResult(false, null,
                    "DSL validation failed: " + String.join("; ", dslValidation.reasons()));
            flightRepo.save(flight);
            return new TestResult(false,
                    "DSL validation failed: " + String.join("; ", dslValidation.reasons()),
                    null);
        }

        flightPlan.markAsInTest();
        flightRepo.save(flight);

        final var json = exporter.exportForSimulator(flightPlan);
        final String reportContent;
        try {
            reportContent = runner.run(json);
        } catch (final SimulationRunnerException e) {
            flightPlan.resetToDraft();
            flightRepo.save(flight);
            return new TestResult(false,
                    "Simulation execution failed: " + e.getMessage(), null);
        }

        final var parsed = ReportParser.parse(reportContent);
        flightPlan.recordTestResult(parsed.isPassed(), null, reportContent);
        flightRepo.save(flight);

        final var msg = parsed.isPassed()
                ? "Flight plan " + flightPlan.identity() + " PASSED - violations: 0"
                : "Flight plan " + flightPlan.identity() + " FAILED - violations: "
                + parsed.violationCount();
        return new TestResult(parsed.isPassed(), msg, reportContent);
    }

    private static DepartureCheckResult checkDepartureTime(final Flight flight,
                                                            final String dslContent) {
        if (dslContent == null) {
            return new DepartureCheckResult("DSL content is null");
        }

        int hours, minutes;
        Matcher matcher = DSL_DEPARTURE_PATTERN.matcher(dslContent);
        if (matcher.find()) {
            hours = Integer.parseInt(matcher.group(1));
            minutes = Integer.parseInt(matcher.group(2));
        } else {
            matcher = ISO_TIME_PATTERN.matcher(dslContent);
            if (matcher.find()) {
                hours = Integer.parseInt(matcher.group(1));
                minutes = Integer.parseInt(matcher.group(2));
            } else {
                return new DepartureCheckResult("Could not parse departure time from DSL");
            }
        }

        final LocalTime dslDepartureTime = LocalTime.of(hours, minutes);

        if (!flight.departureTime().toLocalTime().equals(dslDepartureTime)) {
            return new DepartureCheckResult(
                    "Departure time mismatch: Flight expects "
                            + flight.departureTime().toLocalTime()
                            + " but DSL first leg departs at " + dslDepartureTime);
        }

        return new DepartureCheckResult();
    }

    public Iterable<Flight> allFlights() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return flightRepo.findAll();
    }

    public java.util.List<FlightPlanEntry> allDraftEntries() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        final var result = new java.util.ArrayList<FlightPlanEntry>();
        for (final var flight : flightRepo.findAll()) {
            for (final var fp : flight.flightPlans()) {
                final var st = fp.status();
                if (st != eapli.aisafe.flightplan.domain.FlightPlanStatus.DRAFT
                        && st != eapli.aisafe.flightplan.domain.FlightPlanStatus.TEST_PASSED
                        && st != eapli.aisafe.flightplan.domain.FlightPlanStatus.TEST_FAILED) {
                    continue;
                }
                // Only show user-imported flight plans (plan ID == flight designator),
                // not bootstrap seed data (plan IDs FP001–FP006).
                if (fp.identity().toString().equals(flight.identity().toString())) {
                    result.add(new FlightPlanEntry(flight, fp));
                }
            }
        }
        return result;
    }

    private record DepartureCheckResult(String message) {
        DepartureCheckResult() {
            this(null);
        }

        boolean isPassed() {
            return message == null;
        }
    }
}
