package eapli.aisafe.remote.pilot;

import eapli.aisafe.aircraft.application.ListCompanyFleetController;
import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.CabinConfiguration;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flightplan.application.ImportFlightPlanController;
import eapli.aisafe.flightplan.application.TestFlightPlanController;
import eapli.aisafe.report.application.GenerateMonthlyReportController;
import eapli.aisafe.report.domain.MonthlyReport;
import eapli.aisafe.simulation.application.GenerateSimulationReportController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RemotePilotServiceTest {

    private ListCompanyFleetController fleetCtrl;
    private ImportFlightPlanController importCtrl;
    private TestFlightPlanController testCtrl;
    private GenerateSimulationReportController reportCtrl;
    private GenerateMonthlyReportController monthlyCtrl;
    private RemotePilotService service;

    @BeforeEach
    void setUp() {
        fleetCtrl = mock(ListCompanyFleetController.class);
        importCtrl = mock(ImportFlightPlanController.class);
        testCtrl = mock(TestFlightPlanController.class);
        reportCtrl = mock(GenerateSimulationReportController.class);
        monthlyCtrl = mock(GenerateMonthlyReportController.class);
        service = new RemotePilotService(fleetCtrl, importCtrl, testCtrl, reportCtrl, monthlyCtrl);
    }

    // ── listFleet ──────────────────────────────────────────────────────────

    @Test
    void listFleetDelegatesToController() {
        when(fleetCtrl.allActiveAircraft()).thenReturn(List.of());
        final var result = service.listFleet();
        verify(fleetCtrl).allActiveAircraft();
        assertNotNull(result);
    }

    @Test
    void listFleetReturnsDTOs() {
        final var ac = new Aircraft(
                new RegistrationNumber("CS-TUI", "Portugal"),
                AircraftModelCode.valueOf("A320"),
                CompanyIATA.valueOf("TP"),
                2,
                new CabinConfiguration(List.of(new SeatClass("Economy", 180))),
                LocalDate.of(2018, 6, 15));
        when(fleetCtrl.allActiveAircraft()).thenReturn(List.of(ac));
        final var result = service.listFleet();
        assertEquals(1, result.size());
        assertInstanceOf(AircraftDTO.class, result.get(0));
        assertEquals("CS-TUI", result.get(0).registrationNumber());
    }

    @Test
    void listFleetReturnsEmptyWhenNoAircraft() {
        when(fleetCtrl.allActiveAircraft()).thenReturn(List.of());
        final var result = service.listFleet();
        assertTrue(result.isEmpty());
    }

    // ── createFlightPlan ───────────────────────────────────────────────────

    @Test
    void createFlightPlanDelegatesToImportController() {
        final var expected = new ImportFlightPlanController.DslValidationResult(
                true, List.of(), true, List.of(), true, List.of(), true, "ok", null);
        when(importCtrl.importFlightPlan("valid DSL", "remote-FL123", "FL123"))
                .thenReturn(expected);
        final var result = service.createFlightPlan("FL123", "valid DSL");
        verify(importCtrl).importFlightPlan("valid DSL", "remote-FL123", "FL123");
        assertSame(expected, result);
    }

    @Test
    void createFlightPlanReturnsValidationResult() {
        final var dsl = "departure LIS 10:00; arrival OPO 11:00;";
        final var expected = new ImportFlightPlanController.DslValidationResult(
                true, List.of(), true, List.of(), true, List.of(), true, "ok", null);
        when(importCtrl.importFlightPlan(eq(dsl), startsWith("remote-"), eq("FL001")))
                .thenReturn(expected);
        final var result = service.createFlightPlan("FL001", dsl);
        assertNotNull(result);
    }

    // ── importFlightPlan ─────────────────────────────────────────────────────

    @Test
    void importFlightPlanDelegatesToImportController() {
        final var expected = new ImportFlightPlanController.DslValidationResult(
                true, List.of(), true, List.of(), true, List.of(), true, "ok", null);
        when(importCtrl.importFlightPlan("dsl content", "remote-FP002", "FP002"))
                .thenReturn(expected);
        final var result = service.importFlightPlan("FP002", "dsl content");
        verify(importCtrl).importFlightPlan("dsl content", "remote-FP002", "FP002");
        assertSame(expected, result);
    }

    // ── validateFlightPlan ─────────────────────────────────────────────────

    @Test
    void validateFlightPlanDelegatesToTestController() {
        final var expected = new TestFlightPlanController.TestResult(true, "OK", "report");
        when(testCtrl.testFlightPlan("FP001")).thenReturn(expected);
        final var result = service.validateFlightPlan("FP001");
        verify(testCtrl).testFlightPlan("FP001");
        assertSame(expected, result);
    }

    @Test
    void validateFlightPlanReturnsTestResult() {
        when(testCtrl.testFlightPlan(anyString()))
                .thenReturn(new TestFlightPlanController.TestResult(true, "OK", "report"));
        final var result = service.validateFlightPlan("FP001");
        assertNotNull(result);
    }

    // ── generateReport ─────────────────────────────────────────────────────

    @Test
    void generateReportDelegatesToReportController() {
        when(reportCtrl.generateReport("LPPC")).thenReturn("/tmp/report.txt");
        final var result = service.generateReport("LPPC");
        verify(reportCtrl).generateReport("LPPC");
        assertEquals("/tmp/report.txt", result);
    }

    @Test
    void generateReportReturnsString() {
        when(reportCtrl.generateReport(anyString())).thenReturn("report-path");
        final var result = service.generateReport("LPPC");
        assertInstanceOf(String.class, result);
    }

    // ── monthlyReport ──────────────────────────────────────────────────────

    @Test
    void monthlyReportDelegatesToMonthlyController() {
        final var expected = mock(MonthlyReport.class);
        when(monthlyCtrl.generateForMonth(YearMonth.of(2026, 5))).thenReturn(expected);
        final var result = service.monthlyReport(2026, 5);
        verify(monthlyCtrl).generateForMonth(YearMonth.of(2026, 5));
        assertSame(expected, result);
    }

    @Test
    void monthlyReportReturnsMonthlyReport() {
        when(monthlyCtrl.generateForMonth(any(YearMonth.class)))
                .thenReturn(mock(MonthlyReport.class));
        final var result = service.monthlyReport(2026, 5);
        assertInstanceOf(MonthlyReport.class, result);
    }

    // ── listFlights ────────────────────────────────────────────────────────

    @Test
    void listFlightsDelegatesToTestController() {
        when(testCtrl.allFlights()).thenReturn(List.of());
        final var result = service.listFlights();
        verify(testCtrl).allFlights();
        assertNotNull(result);
    }

    @Test
    void listFlightsReturnsList() {
        final var flight = mock(Flight.class);
        when(testCtrl.allFlights()).thenReturn(List.of(flight));
        final var result = service.listFlights();
        assertEquals(1, result.size());
    }
}
