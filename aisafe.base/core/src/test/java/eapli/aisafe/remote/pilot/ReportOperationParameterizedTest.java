package eapli.aisafe.remote.pilot;

import eapli.aisafe.aircraft.application.ListCompanyFleetController;
import eapli.aisafe.flightplan.application.ImportFlightPlanController;
import eapli.aisafe.flightplan.application.TestFlightPlanController;
import eapli.aisafe.report.application.GenerateMonthlyReportController;
import eapli.aisafe.report.domain.MonthlyReport;
import eapli.aisafe.simulation.application.GenerateSimulationReportController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportOperationParameterizedTest {

    private GenerateSimulationReportController reportCtrl;
    private GenerateMonthlyReportController monthlyCtrl;
    private RemotePilotService service;

    @BeforeEach
    void setUp() {
        reportCtrl = mock(GenerateSimulationReportController.class);
        monthlyCtrl = mock(GenerateMonthlyReportController.class);
        service = new RemotePilotService(
                mock(ListCompanyFleetController.class),
                mock(ImportFlightPlanController.class),
                mock(TestFlightPlanController.class),
                reportCtrl,
                monthlyCtrl);
    }

    @ParameterizedTest(name = "{0}: {4}")
    @MethodSource("csvTestData")
    void reportOperationScenarios(
            final String testCaseId,
            final String operation,
            final String param1,
            final String param2,
            final String scenario,
            final String expectedOutcome) {

        switch (operation) {
            case "GENERATE_REPORT" -> testGenerateReport(
                    testCaseId, param1, expectedOutcome);
            case "MONTHLY_REPORT" -> testMonthlyReport(
                    testCaseId, param1, param2, expectedOutcome);
        }
    }

    private void testGenerateReport(final String testCaseId,
                                    final String areaCode,
                                    final String expectedOutcome) {
        if ("FAILURE".equals(expectedOutcome)) {
            if (areaCode == null || areaCode.isEmpty()) {
                when(reportCtrl.generateReport(null))
                        .thenThrow(new IllegalArgumentException("Area code must not be null"));
                assertThrows(IllegalArgumentException.class,
                        () -> service.generateReport(null),
                        testCaseId + " should throw on null area code");
            } else {
                when(reportCtrl.generateReport(areaCode))
                        .thenThrow(new IllegalArgumentException("No data for area: " + areaCode));
                assertThrows(IllegalArgumentException.class,
                        () -> service.generateReport(areaCode),
                        testCaseId + " should throw on unknown area");
            }
        } else {
            when(reportCtrl.generateReport(areaCode)).thenReturn("/tmp/report.txt");
            final var result = service.generateReport(areaCode);
            assertNotNull(result, testCaseId + " should return a path");
        }
    }

    private void testMonthlyReport(final String testCaseId,
                                   final String yearStr,
                                   final String monthStr,
                                   final String expectedOutcome) {
        if ("FAILURE".equals(expectedOutcome)) {
            final int year = Integer.parseInt(yearStr);
            final int month = Integer.parseInt(monthStr);
            if (month < 1 || month > 12) {
                assertThrows(Exception.class,
                        () -> service.monthlyReport(year, month),
                        testCaseId + " should throw on invalid month");
            } else {
                when(monthlyCtrl.generateForMonth(YearMonth.of(year, month)))
                        .thenThrow(new IllegalArgumentException("Invalid period"));
                assertThrows(IllegalArgumentException.class,
                        () -> service.monthlyReport(year, month),
                        testCaseId + " should throw on invalid period");
            }
        } else {
            final int year = Integer.parseInt(yearStr);
            final int month = Integer.parseInt(monthStr);
            when(monthlyCtrl.generateForMonth(YearMonth.of(year, month)))
                    .thenReturn(mock(MonthlyReport.class));
            final var result = service.monthlyReport(year, month);
            assertNotNull(result, testCaseId + " should return a report");
        }
    }

    private static Stream<Arguments> csvTestData() {
        final var rows = new ArrayList<Arguments>();
        try (var reader = new BufferedReader(new InputStreamReader(
                ReportOperationParameterizedTest.class.getResourceAsStream(
                        "/report_operation_test_data.csv"),
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
