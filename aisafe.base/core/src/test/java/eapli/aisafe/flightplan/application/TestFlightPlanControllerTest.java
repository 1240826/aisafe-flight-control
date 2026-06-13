package eapli.aisafe.flightplan.application;

import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.flightplan.domain.FlightPlan;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.flightplan.domain.FlightPlanStatus;
import eapli.aisafe.flightplan.domain.ValidationResult;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TestFlightPlanControllerTest {

    private static final LocalDateTime DEP_TIME = LocalDateTime.of(2026, 6, 2, 10, 0);

    private AuthorizationService authz;
    private FlightRepository flightRepo;
    private FlightPlanExporter exporter;
    private SimulationRunner runner;
    private DslValidator dslValidator;
    private WeatherDataRepository weatherRepo;
    private TestFlightPlanController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        flightRepo = mock(FlightRepository.class);
        exporter = mock(FlightPlanExporter.class);
        runner = mock(SimulationRunner.class);
        dslValidator = mock(DslValidator.class);
        weatherRepo = mock(WeatherDataRepository.class);
        controller = new TestFlightPlanController(authz, flightRepo, exporter, runner, dslValidator, weatherRepo);
    }

    @Test
    void ensureTestFlightPlanWithUnknownIdThrows() {
        when(flightRepo.findByFlightPlanId(any())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> controller.testFlightPlan("UNKNOWN"));
    }

    @Test
    void ensureTestFlightPlanInvalidIdThrows() {
        assertThrows(IllegalStateException.class,
                () -> controller.testFlightPlan("   "));
    }

    @Test
    void ensureTestFlightPlanNotInDraftReturnsFailure() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "departure LIS 10:00; valid DSL");
        fp.markAsInTest();
        when(flightRepo.findByFlightPlanId(FlightPlanId.valueOf("FP001")))
                .thenReturn(Optional.of(flight));

        final var result = controller.testFlightPlan("FP001");
        assertFalse(result.passed());
        assertTrue(result.message().contains("not in DRAFT status"));
    }

    @Test
    void ensureTestFlightPlanDslFailureReturnsFailure() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        when(flightRepo.findByFlightPlanId(FlightPlanId.valueOf("FP001")))
                .thenReturn(Optional.of(flight));
        when(dslValidator.validate(any())).thenReturn(
                ValidationResult.failed("DSL must start with 'departure'"));

        final var result = controller.testFlightPlan("FP001");
        assertFalse(result.passed());
        assertTrue(result.message().contains("DSL validation failed"));
        assertEquals(FlightPlanStatus.TEST_FAILED, fp.status(),
                "US085.4: DSL failure must set status to TEST_FAILED");
        verify(flightRepo, times(1)).save(any());
    }

    @Test
    void ensureTestFlightPlanRunnerFailureResetsToDraft() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        when(flightRepo.findByFlightPlanId(FlightPlanId.valueOf("FP001")))
                .thenReturn(Optional.of(flight));
        when(dslValidator.validate(any())).thenReturn(ValidationResult.passed());
        when(exporter.exportForSimulator(any())).thenReturn("{}");
        when(runner.run(any()))
                .thenThrow(new SimulationRunnerException(
                        "Simulator crashed"));

        final var result = controller.testFlightPlan("FP001");
        assertFalse(result.passed());
        assertEquals(FlightPlanStatus.DRAFT, fp.status(),
                "Flight plan should be reset to DRAFT after runner failure");
        verify(flightRepo, times(2)).save(any());
    }

    @Test
    void ensureTestFlightPlanWithDesignatorAndIdPassHappyPath() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP1234")))
                .thenReturn(Optional.of(flight));
        when(dslValidator.validate(any())).thenReturn(ValidationResult.passed());
        when(exporter.exportForSimulator(any())).thenReturn("{}");
        when(runner.run(any())).thenReturn(generatePassReport());

        final var result = controller.testFlightPlan("TP1234", "FP001");
        assertTrue(result.passed());
        assertEquals(FlightPlanStatus.TEST_PASSED, fp.status());
        assertNotNull(result.reportContent());
    }

    @Test
    void ensureTestFlightPlanPassHappyPath() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        when(flightRepo.findByFlightPlanId(FlightPlanId.valueOf("FP001")))
                .thenReturn(Optional.of(flight));
        when(dslValidator.validate(any())).thenReturn(ValidationResult.passed());
        when(exporter.exportForSimulator(any())).thenReturn("{}");
        when(runner.run(any())).thenReturn(generatePassReport());

        final var result = controller.testFlightPlan("FP001");
        assertTrue(result.passed());
        assertEquals(FlightPlanStatus.TEST_PASSED, fp.status());
        assertNotNull(result.reportContent());
    }

    @Test
    void ensureTestFlightPlanFailHappyPath() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        when(flightRepo.findByFlightPlanId(FlightPlanId.valueOf("FP001")))
                .thenReturn(Optional.of(flight));
        when(dslValidator.validate(any())).thenReturn(ValidationResult.passed());
        when(exporter.exportForSimulator(any())).thenReturn("{}");
        when(runner.run(any())).thenReturn(generateFailReport(2));

        final var result = controller.testFlightPlan("FP001");
        assertFalse(result.passed());
        assertEquals(FlightPlanStatus.TEST_FAILED, fp.status());
        assertTrue(result.message().contains("FAILED"));
        assertTrue(result.message().contains("violations: 2"));
    }

    @Test
    void ensureAllFlightsDelegates() {
        when(flightRepo.findAll()).thenReturn(java.util.List.of());
        controller.allFlights();
        verify(flightRepo).findAll();
    }

    @Test
    void ensureAuthorizationIsChecked() {
        when(flightRepo.findAll()).thenReturn(java.util.List.of());
        assertThrows(Exception.class, () -> {
            doThrow(new SecurityException("Not authorized"))
                    .when(authz).ensureAuthenticatedUserHasAnyOf(any());
            controller.allFlights();
        });
    }

    @Test
    void ensureDepartureTimeMismatchReturnsFailure() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"),
                LocalDateTime.of(2026, 6, 2, 14, 30));
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        when(flightRepo.findByFlightPlanId(FlightPlanId.valueOf("FP001")))
                .thenReturn(Optional.of(flight));

        final var result = controller.testFlightPlan("FP001");
        assertFalse(result.passed());
        assertTrue(result.message().contains("Departure time mismatch"));
    }

    @Test
    void ensureMissingDslDepartureTimeReturnsFailure() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "arrival OPO 11:00; type regular;");
        when(flightRepo.findByFlightPlanId(FlightPlanId.valueOf("FP001")))
                .thenReturn(Optional.of(flight));

        final var result = controller.testFlightPlan("FP001");
        assertFalse(result.passed());
        assertTrue(result.message().contains("Could not parse departure time"));
    }

    @Test
    void ensureScenarioWithOneEntryPasses() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        when(dslValidator.validate(any())).thenReturn(ValidationResult.passed());
        when(exporter.exportForSimulator(any())).thenReturn("[{\"ID\":\"T1\"}]");
        when(runner.run(any())).thenReturn(generatePassReport());

        final var entry = new TestFlightPlanController.FlightPlanEntry(flight, fp);
        final var result = controller.testScenario(List.of(entry));
        assertTrue(result.passed());
        assertEquals(FlightPlanStatus.TEST_PASSED, fp.status());
        assertNotNull(result.reportContent());
    }

    @Test
    void ensureScenarioWithMultipleEntriesPasses() {
        final var flight1 = new Flight(FlightDesignator.valueOf("TP100"), DEP_TIME);
        final var fp1 = flight1.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        final var flight2 = new Flight(FlightDesignator.valueOf("TP200"), DEP_TIME);
        final var fp2 = flight2.addFlightPlan(FlightPlanId.valueOf("FP002"),
                "departure LIS 10:00; arrival OPO 11:00");
        when(dslValidator.validate(any())).thenReturn(ValidationResult.passed());
        when(exporter.exportForSimulator(any())).thenReturn("[{\"ID\":\"T1\"}]");
        when(runner.run(any())).thenReturn(generatePassReport());

        final var entry1 = new TestFlightPlanController.FlightPlanEntry(flight1, fp1);
        final var entry2 = new TestFlightPlanController.FlightPlanEntry(flight2, fp2);
        final var result = controller.testScenario(List.of(entry1, entry2));
        assertTrue(result.passed());
        assertEquals(FlightPlanStatus.TEST_PASSED, fp1.status());
        assertEquals(FlightPlanStatus.TEST_PASSED, fp2.status());
        assertEquals(2, result.results().size());
    }

    @Test
    void ensureScenarioSkipsInvalidStatus() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        fp.markAsInTest(); // status is now IN_TEST — not valid for scenario

        final var entry = new TestFlightPlanController.FlightPlanEntry(flight, fp);
        final var result = controller.testScenario(List.of(entry));
        assertFalse(result.passed());
        assertTrue(result.message().contains("No valid flight plans"));
    }

    @Test
    void ensureScenarioRunnerFailureResetsToDraft() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        when(dslValidator.validate(any())).thenReturn(ValidationResult.passed());
        when(exporter.exportForSimulator(any())).thenReturn("{}");
        when(runner.run(any()))
                .thenThrow(new SimulationRunnerException("Simulator crashed"));

        final var entry = new TestFlightPlanController.FlightPlanEntry(flight, fp);
        final var result = controller.testScenario(List.of(entry));
        assertFalse(result.passed());
        assertEquals(FlightPlanStatus.DRAFT, fp.status(),
                "Should reset to DRAFT after runner failure");
    }

    @Test
    void ensureScenarioSkipsDslValidationFailure() {
        final var flight1 = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp1 = flight1.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        final var flight2 = new Flight(FlightDesignator.valueOf("TP5678"), DEP_TIME);
        final var fp2 = flight2.addFlightPlan(FlightPlanId.valueOf("FP002"),
                "departure LIS 10:00; arrival OPO 11:00");
        when(dslValidator.validate(any())).thenReturn(ValidationResult.passed())
                .thenReturn(ValidationResult.failed("Invalid DSL"));
        when(exporter.exportForSimulator(any())).thenReturn("[{\"ID\":\"T1\"}]");
        when(runner.run(any())).thenReturn(generatePassReport());

        final var entry1 = new TestFlightPlanController.FlightPlanEntry(flight1, fp1);
        final var entry2 = new TestFlightPlanController.FlightPlanEntry(flight2, fp2);
        final var result = controller.testScenario(List.of(entry1, entry2));
        assertTrue(result.passed());
        assertEquals(FlightPlanStatus.TEST_PASSED, fp1.status());
        assertEquals(FlightPlanStatus.DRAFT, fp2.status(),
                "Failed validation should NOT be marked IN_TEST");
    }

    private String generatePassReport() {
        return """
                ============================================
                  AISafe Simulation Report
                  Generated: Mon Jun  1 12:00:00 2026
                  Total steps: 7200  (7200 seconds simulated)
                  Flights: 1
                  Total violations detected: 0
                ============================================

                FLIGHT SUMMARY:
                  TP0123: n_viol=0  ever_in_area=yes  completed=yes

                ============================================
                  RESULT: PASS
                ============================================
                """;
    }

    private String generateFailReport(final int violations) {
        return String.format("""
                ============================================
                  AISafe Simulation Report
                  Generated: Mon Jun  1 12:00:00 2026
                  Total steps: 7200  (7200 seconds simulated)
                  Flights: 1
                  Total violations detected: %d
                ============================================

                FLIGHT SUMMARY:
                  TP0123: n_viol=%d  ever_in_area=yes  completed=yes

                ============================================
                  RESULT: FAIL
                ============================================
                """, violations, violations);
    }

    @Test
    void ensureTestFlightPlanWithDesignatorFlightNotFoundThrows() {
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP9999")))
                .thenReturn(java.util.Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> controller.testFlightPlan("TP9999", "FP001"));
    }

    @Test
    void ensureAllTestedEntriesReturnsOnlyTestedPlans() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        fp.markAsInTest();
        fp.recordTestResult(true, null, "report content");
        when(flightRepo.findAll()).thenReturn(List.of(flight));

        final var result = controller.allTestedEntries();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void ensureAllTestedEntriesSkipsNonTestedPlans() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        flight.addFlightPlan(FlightPlanId.valueOf("FP001"),
                "departure LIS 10:00; arrival OPO 11:00");
        when(flightRepo.findAll()).thenReturn(List.of(flight));

        final var result = controller.allTestedEntries();
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureAllTestedEntriesChecksAuthorization() {
        when(flightRepo.findAll()).thenReturn(List.of());
        controller.allTestedEntries();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureAllDraftEntriesReturnsDraftPlans() {
        final var flight = new Flight(FlightDesignator.valueOf("TP1234"), DEP_TIME);
        final var fp = flight.addFlightPlan(FlightPlanId.valueOf("TP1234"),
                "departure LIS 10:00; arrival OPO 11:00");
        when(flightRepo.findAll()).thenReturn(List.of(flight));

        final var result = controller.allDraftEntries();
        assertFalse(result.isEmpty());
    }

    @Test
    void ensureAllDraftEntriesChecksAuthorization() {
        when(flightRepo.findAll()).thenReturn(List.of());
        controller.allDraftEntries();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }
}
