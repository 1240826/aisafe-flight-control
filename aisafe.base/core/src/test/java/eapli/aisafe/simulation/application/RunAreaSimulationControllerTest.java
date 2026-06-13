package eapli.aisafe.simulation.application;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.flightplan.application.FlightPlanExporter;
import eapli.aisafe.flightplan.application.SimulationRunner;
import eapli.aisafe.flightplan.application.SimulationRunnerException;
import eapli.aisafe.flightplan.domain.FlightPlan;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.simulation.domain.Simulation;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RunAreaSimulationControllerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 14, 0, 0);
    private static final LocalDateTime END   = LocalDateTime.of(2026, 5, 14, 6, 0);

    private AuthorizationService authz;
    private FlightRepository flightRepo;
    private FlightPlanExporter exporter;
    private SimulationRunner runner;
    private AirControlAreaRepository acaRepo;
    private SaveSimulationController saveCtrl;
    private RunAreaSimulationController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        flightRepo = mock(FlightRepository.class);
        exporter = mock(FlightPlanExporter.class);
        runner = mock(SimulationRunner.class);
        acaRepo = mock(AirControlAreaRepository.class);
        saveCtrl = mock(SaveSimulationController.class);
        controller = new RunAreaSimulationController(authz, flightRepo, exporter, runner, acaRepo, saveCtrl);
    }

    private Flight makeFlightWithPlan(final String designator, final LocalDateTime depTime) {
        final Flight flight = new Flight(FlightDesignator.valueOf(designator), depTime);
        flight.addFlightPlan(FlightPlanId.valueOf(designator), "departure LIS 10:00; arrival OPO 11:00");
        return flight;
    }

    @Test
    void ensureAvailableAreasDelegatesToRepo() {
        when(acaRepo.findAll()).thenReturn(List.of(mock(AirControlArea.class)));
        final Iterable<AirControlArea> result = controller.availableAreas();
        verify(acaRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAvailableAreasChecksAuthorization() {
        when(acaRepo.findAll()).thenReturn(List.of());
        controller.availableAreas();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    @Test
    void ensureRunSimulationSavesAndReturnsSimulation() {
        final Flight flight = makeFlightWithPlan("TP1234", LocalDateTime.of(2026, 5, 14, 2, 0));
        when(flightRepo.findAll()).thenReturn(List.of(flight));
        when(exporter.exportForSimulator(any())).thenReturn("[{\"id\":\"T1\"}]");
        when(runner.run(any())).thenReturn("SIMULATION REPORT: OK");
        final Simulation expected = mock(Simulation.class);
        when(saveCtrl.saveSimulation(any(), any(), any(), anyDouble(), any(), any(), any()))
                .thenReturn(expected);

        final Simulation result = controller.runSimulation("LPPC", START, END, 500.0, "kg/h");

        assertSame(expected, result);
        verify(runner).run(any());
        verify(saveCtrl).saveSimulation(any(), any(), any(), anyDouble(), any(), any(), any());
    }

    @Test
    void ensureRunSimulationChecksAuthorization() {
        when(flightRepo.findAll()).thenReturn(List.of());
        assertThrows(IllegalStateException.class,
                () -> controller.runSimulation("LPPC", START, END, 500.0, "kg/h"));
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    @Test
    void ensureRunSimulationThrowsWhenNoFlightsFound() {
        when(flightRepo.findAll()).thenReturn(List.of());
        assertThrows(IllegalStateException.class,
                () -> controller.runSimulation("LPPC", START, END, 500.0, "kg/h"));
    }

    @Test
    void ensureRunSimulationThrowsWhenRunnerFails() {
        final Flight flight = makeFlightWithPlan("TP1234", LocalDateTime.of(2026, 5, 14, 2, 0));
        when(flightRepo.findAll()).thenReturn(List.of(flight));
        when(exporter.exportForSimulator(any())).thenReturn("[{}]");
        when(runner.run(any())).thenThrow(new SimulationRunnerException("Simulator crashed"));

        assertThrows(IllegalStateException.class,
                () -> controller.runSimulation("LPPC", START, END, 500.0, "kg/h"));
    }

    @Test
    void ensureRunSimulationSkipsFlightsWithoutDepartureTime() {
        final Flight nullDepFlight1 = mock(Flight.class);
        when(nullDepFlight1.departureTime()).thenReturn(null);
        final Flight nullDepFlight2 = mock(Flight.class);
        when(nullDepFlight2.departureTime()).thenReturn(null);

        when(flightRepo.findAll()).thenReturn(List.of(nullDepFlight1, nullDepFlight2));

        assertThrows(IllegalStateException.class,
                () -> controller.runSimulation("LPPC", START, END, 500.0, "kg/h"));
    }

    @Test
    void ensureRunSimulationSkipsFlightsOutsideTimeWindow() {
        final Flight earlyFlight = makeFlightWithPlan("TP100", LocalDateTime.of(2026, 5, 13, 23, 0));
        final Flight lateFlight = makeFlightWithPlan("TP200", LocalDateTime.of(2026, 5, 14, 7, 0));
        when(flightRepo.findAll()).thenReturn(List.of(earlyFlight, lateFlight));

        assertThrows(IllegalStateException.class,
                () -> controller.runSimulation("LPPC", START, END, 500.0, "kg/h"));
    }

    @Test
    void ensureRunSimulationSkipsFlightsWithoutFlightPlans() {
        final Flight noPlanFlight = new Flight(FlightDesignator.valueOf("TP500"), LocalDateTime.of(2026, 5, 14, 2, 0));
        when(flightRepo.findAll()).thenReturn(List.of(noPlanFlight));

        assertThrows(IllegalStateException.class,
                () -> controller.runSimulation("LPPC", START, END, 500.0, "kg/h"));
    }

    @Test
    void ensureRunSimulationHandlesMultipleFlights() {
        final Flight f1 = makeFlightWithPlan("TP100", LocalDateTime.of(2026, 5, 14, 1, 0));
        final Flight f2 = makeFlightWithPlan("TP200", LocalDateTime.of(2026, 5, 14, 3, 0));
        when(flightRepo.findAll()).thenReturn(List.of(f1, f2));
        when(exporter.exportForSimulator(any())).thenReturn("[{}]");
        when(runner.run(any())).thenReturn("REPORT OK");
        when(saveCtrl.saveSimulation(any(), any(), any(), anyDouble(), any(), any(), any()))
                .thenReturn(mock(Simulation.class));

        final Simulation result = controller.runSimulation("LPPC", START, END, 500.0, "kg/h");

        assertNotNull(result);
        verify(exporter, times(2)).exportForSimulator(any());
    }
}
