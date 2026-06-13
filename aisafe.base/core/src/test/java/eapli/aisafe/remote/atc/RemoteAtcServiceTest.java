package eapli.aisafe.remote.atc;

import eapli.aisafe.aircraft.application.AddAircraftController;
import eapli.aisafe.aircraft.application.DecommissionAircraftController;
import eapli.aisafe.aircraft.application.ListCompanyFleetController;
import eapli.aisafe.flightroute.application.CreateFlightRouteController;
import eapli.aisafe.flightroute.application.DeleteFlightRouteController;
import eapli.aisafe.pilot.application.AddPilotController;
import eapli.aisafe.pilot.application.ListPilotRosterController;
import eapli.aisafe.pilot.application.RemovePilotController;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RemoteAtcServiceTest {

    private AddAircraftController addAircraftCtrl;
    private DecommissionAircraftController decommAircraftCtrl;
    private ListCompanyFleetController listFleetCtrl;
    private CreateFlightRouteController createRouteCtrl;
    private DeleteFlightRouteController deleteRouteCtrl;
    private AddPilotController addPilotCtrl;
    private ListPilotRosterController listPilotsCtrl;
    private RemovePilotController removePilotCtrl;
    private RemoteAtcService service;

    @BeforeEach
    void setUp() {
        addAircraftCtrl = mock(AddAircraftController.class);
        decommAircraftCtrl = mock(DecommissionAircraftController.class);
        listFleetCtrl = mock(ListCompanyFleetController.class);
        createRouteCtrl = mock(CreateFlightRouteController.class);
        deleteRouteCtrl = mock(DeleteFlightRouteController.class);
        addPilotCtrl = mock(AddPilotController.class);
        listPilotsCtrl = mock(ListPilotRosterController.class);
        removePilotCtrl = mock(RemovePilotController.class);
        service = new RemoteAtcService(addAircraftCtrl, decommAircraftCtrl,
                listFleetCtrl, createRouteCtrl, deleteRouteCtrl,
                addPilotCtrl, listPilotsCtrl, removePilotCtrl);
    }

    @Test
    void addAircraftDelegatesToController() {
        final var result = service.addAircraft("CS-TUI", "Portugal", "A320", "TP", 2, "2026-06-15");
        verify(addAircraftCtrl).addAircraft(eq("CS-TUI"), eq("Portugal"), eq("A320"), eq("TP"),
                eq(2), anyList(), any());
        assertTrue(result.contains("CS-TUI"));
    }

    @Test
    void decommissionAircraftDelegatesToController() {
        final var result = service.decommissionAircraft("CS-TUI", "Portugal");
        verify(decommAircraftCtrl).decommissionAircraft("CS-TUI", "Portugal");
        assertTrue(result.contains("CS-TUI"));
    }

    @Test
    void listFleetDelegatesToController() {
        when(listFleetCtrl.allActiveAircraft()).thenReturn(java.util.List.of());
        final var result = service.listFleet();
        verify(listFleetCtrl).allActiveAircraft();
        assertTrue(result.contains("aircraft"));
    }

    @Test
    void createRouteDelegatesToController() {
        final var result = service.createRoute("TP123", "TP", "OPO", "LIS");
        verify(createRouteCtrl).createFlightRoute("TP123", "TP", "OPO", "LIS");
        assertTrue(result.contains("TP123"));
    }

    @Test
    void deleteRouteDelegatesToController() {
        final var result = service.deleteRoute("TP123", "2026-06-01");
        verify(deleteRouteCtrl).deactivateRoute("TP123", java.time.LocalDate.of(2026, 6, 1));
        assertTrue(result.contains("TP123"));
    }

    @Test
    void addPilotDelegatesToController() {
        final var result = service.addPilot("P12345", "TP", "2026-06-15", "A320,B738");
        verify(addPilotCtrl).addPilot(eq("P12345"),
                eq(eapli.aisafe.company.domain.CompanyIATA.valueOf("TP")),
                anySet(), any());
        assertTrue(result.contains("P12345"));
    }

    @Test
    void listPilotsDelegatesToController() {
        when(listPilotsCtrl.listCompanyPilots(any())).thenReturn(java.util.List.of());
        final var result = service.listPilots("TP");
        verify(listPilotsCtrl).listCompanyPilots(
                eq(eapli.aisafe.company.domain.CompanyIATA.valueOf("TP")));
        assertTrue(result.contains("pilots"));
    }

    @Test
    void removePilotDelegatesToController() {
        final var result = service.removePilot("P12345");
        verify(removePilotCtrl).deactivatePilot(PilotId.valueOf("P12345"));
        assertTrue(result.contains("P12345"));
    }

    @Test
    void listRoutesDelegatesToController() {
        when(deleteRouteCtrl.activeRoutes()).thenReturn(java.util.List.of());
        final var result = service.listRoutes();
        verify(deleteRouteCtrl).activeRoutes();
        assertTrue(result.contains("routes"));
    }
}
