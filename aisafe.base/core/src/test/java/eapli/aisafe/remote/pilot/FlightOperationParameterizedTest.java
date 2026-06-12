package eapli.aisafe.remote.pilot;

import eapli.aisafe.aircraft.application.ListCompanyFleetController;
import eapli.aisafe.flightplan.application.ImportFlightPlanController;
import eapli.aisafe.flightplan.application.TestFlightPlanController;
import eapli.aisafe.flightroute.application.ListFlightRoutesController;
import eapli.aisafe.report.application.GenerateMonthlyReportController;
import eapli.aisafe.simulation.application.GenerateSimulationReportController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FlightOperationParameterizedTest {

    private ImportFlightPlanController importCtrl;
    private TestFlightPlanController testCtrl;
    private RemotePilotService service;

    @BeforeEach
    void setUp() {
        importCtrl = mock(ImportFlightPlanController.class);
        testCtrl = mock(TestFlightPlanController.class);
        service = new RemotePilotService(
                mock(ListCompanyFleetController.class),
                importCtrl,
                testCtrl,
                mock(GenerateSimulationReportController.class),
                mock(GenerateMonthlyReportController.class),
                mock(ListFlightRoutesController.class));
    }

    @ParameterizedTest(name = "{0}: {4}")
    @MethodSource("csvTestData")
    void flightOperationScenarios(
            final String testCaseId,
            final String operation,
            final String flightId,
            final String dsl,
            final String scenario,
            final String expectedOutcome) {

        switch (operation) {
            case "CREATE_FLIGHT_PLAN" -> testCreateFlightPlan(
                    testCaseId, flightId, dsl, expectedOutcome);
            case "IMPORT_FLIGHT_PLAN" -> testImportFlightPlan(
                    testCaseId, flightId, dsl, expectedOutcome);
            case "VALIDATE_FLIGHT_PLAN" -> testValidateFlightPlan(
                    testCaseId, flightId, expectedOutcome);
        }
    }

    private void testCreateFlightPlan(final String testCaseId,
                                      final String flightId,
                                      final String dsl,
                                      final String expectedOutcome) {
        if ("FAILURE".equals(expectedOutcome)) {
            when(importCtrl.importFlightPlan(anyString(), anyString(), anyString()))
                    .thenThrow(new IllegalArgumentException("Invalid DSL"));
            assertThrows(IllegalArgumentException.class,
                    () -> service.createFlightPlan(flightId, dsl),
                    testCaseId + " should throw on invalid input");
        } else {
            final var expected = new ImportFlightPlanController.DslValidationResult(
                    true, List.of(), true, List.of(), true, List.of(), true, "ok", null);
            when(importCtrl.importFlightPlan(eq(dsl), startsWith("remote-"), eq(flightId)))
                    .thenReturn(expected);
            final var result = service.createFlightPlan(flightId, dsl);
            assertNotNull(result, testCaseId + " should return a result");
        }
    }

    private void testImportFlightPlan(final String testCaseId,
                                      final String flightId,
                                      final String dsl,
                                      final String expectedOutcome) {
        if ("FAILURE".equals(expectedOutcome)) {
            when(importCtrl.importFlightPlan(anyString(), anyString(), anyString()))
                    .thenThrow(new IllegalArgumentException("Invalid DSL"));
            assertThrows(IllegalArgumentException.class,
                    () -> service.importFlightPlan(flightId, dsl),
                    testCaseId + " should throw on invalid input");
        } else {
            final var expected = new ImportFlightPlanController.DslValidationResult(
                    true, List.of(), true, List.of(), true, List.of(), true, "ok", null);
            when(importCtrl.importFlightPlan(eq(dsl), startsWith("remote-"), eq(flightId)))
                    .thenReturn(expected);
            final var result = service.importFlightPlan(flightId, dsl);
            assertNotNull(result, testCaseId + " should return a result");
        }
    }

    private void testValidateFlightPlan(final String testCaseId,
                                        final String flightPlanId,
                                        final String expectedOutcome) {
        if ("FAILURE".equals(expectedOutcome)) {
            when(testCtrl.testFlightPlan(flightPlanId))
                    .thenReturn(new TestFlightPlanController.TestResult(
                            false, "Flight plan not found", ""));
            final var result = service.validateFlightPlan(flightPlanId);
            assertFalse(result.passed(), testCaseId + " should fail");
        } else {
            when(testCtrl.testFlightPlan(flightPlanId))
                    .thenReturn(new TestFlightPlanController.TestResult(
                            true, "All checks passed", "report content"));
            final var result = service.validateFlightPlan(flightPlanId);
            assertTrue(result.passed(), testCaseId + " should pass");
        }
    }

    private static Stream<Arguments> csvTestData() {
        final var rows = new ArrayList<Arguments>();
        try (var reader = new BufferedReader(new InputStreamReader(
                FlightOperationParameterizedTest.class.getResourceAsStream(
                        "/flight_operation_test_data.csv"),
                StandardCharsets.UTF_8))) {
            final var lines = reader.lines().toList();
            for (final var line : lines) {
                if (line.isBlank()) continue;
                if (line.startsWith("testCaseId")) continue;
                final var parts = line.split(",", 6);
                if (parts.length < 6) continue;
                rows.add(Arguments.of(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        parts[3].trim(),
                        parts[4].trim(),
                        parts[5].trim()));
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to load CSV test data", e);
        }
        return rows.stream();
    }
}
