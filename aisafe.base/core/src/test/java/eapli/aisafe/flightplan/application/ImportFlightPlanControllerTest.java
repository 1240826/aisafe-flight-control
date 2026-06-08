package eapli.aisafe.flightplan.application;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.flightplan.domain.FlightPlanStatus;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import eapli.aisafe.flight.domain.FlightDesignator;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportFlightPlanControllerTest {

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

    private static final String INVALID_DSL = "invalid content";

    private AuthorizationService authz;
    private FlightRepository flightRepo;
    private FlightRouteRepository flightRouteRepo;
    private AircraftRepository aircraftRepo;
    private PilotRepository pilotRepo;
    private ImportFlightPlanController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        flightRepo = mock(FlightRepository.class);
        flightRouteRepo = mock(FlightRouteRepository.class);
        aircraftRepo = mock(AircraftRepository.class);
        pilotRepo = mock(PilotRepository.class);
        controller = new ImportFlightPlanController(authz, flightRepo,
                flightRouteRepo, aircraftRepo, pilotRepo);
        doNothing().when(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureValidDslCreatesFlightPlan() {
        // Route lookup
        final var route = new FlightRoute(
                FlightRouteName.valueOf("TP999"),
                CompanyIATA.valueOf("TP"),
                AirportIATA.valueOf("LIS"),
                AirportIATA.valueOf("CDG"));
        when(flightRouteRepo.findByOriginAndDestinationAndCompany(
                AirportIATA.valueOf("LIS"),
                AirportIATA.valueOf("CDG"),
                CompanyIATA.valueOf("TP")))
                .thenReturn(Optional.of(route));

        // Aircraft lookup
        final var aircraft = mock(Aircraft.class);
        when(aircraft.isActive()).thenReturn(true);
        when(aircraftRepo.findByRegistrationNumber(
                RegistrationNumber.valueOf("CS-TUB", "PT")))
                .thenReturn(Optional.of(aircraft));

        // Pilot lookup
        final var pilot = mock(Pilot.class);
        when(pilot.isActive()).thenReturn(true);
        when(pilot.company()).thenReturn(CompanyIATA.valueOf("TP"));
        when(pilotRepo.findByLicenseNumber(PilotId.valueOf("P12345")))
                .thenReturn(Optional.of(pilot));

        // No existing flight — will be created
        when(flightRepo.ofIdentity(FlightDesignator.valueOf("TP3000")))
                .thenReturn(Optional.empty());

        final var result = controller.importFlightPlan(VALID_DSL, "test.flightplan", "FP001");

        assertTrue(result.allPassed());
        assertNotNull(result.flightPlan());
        assertEquals("FP001", result.flightPlan().identity().toString());
        assertEquals(FlightPlanStatus.DRAFT, result.flightPlan().status());
        assertNotNull(result.summary());
        verify(flightRepo).save(any());
    }

    @Test
    void ensureInvalidDslReturnsErrors() {
        final var result = controller.importFlightPlan(INVALID_DSL, "test.flightplan", "FP001");

        assertFalse(result.allPassed(), "All-passed should be false for invalid DSL");
        assertNull(result.flightPlan(), "No flight plan should be created");
        assertFalse(result.syntacticPassed(), "Syntactic analysis should fail");
        assertFalse(result.allErrors().isEmpty(), "Should have error messages");
    }

    @Test
    void ensureMissingRouteReturnsError() {
        when(flightRouteRepo.findByOriginAndDestinationAndCompany(
                AirportIATA.valueOf("LIS"),
                AirportIATA.valueOf("CDG"),
                CompanyIATA.valueOf("TP")))
                .thenReturn(Optional.empty());

        final var result = controller.importFlightPlan(VALID_DSL, "test.flightplan", "FP001");

        assertFalse(result.allPassed());
        assertFalse(result.semanticPassed());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.contains("No active route found")),
                "Should contain 'No active route found' in semantic errors");
        assertNull(result.flightPlan());
    }

    @Test
    void ensureMissingPilotReturnsError() {
        // Route + aircraft pass, but pilot is missing
        final var route = new FlightRoute(
                FlightRouteName.valueOf("TP999"),
                CompanyIATA.valueOf("TP"),
                AirportIATA.valueOf("LIS"),
                AirportIATA.valueOf("CDG"));
        when(flightRouteRepo.findByOriginAndDestinationAndCompany(any(), any(), any()))
                .thenReturn(Optional.of(route));

        final var aircraft = mock(Aircraft.class);
        when(aircraft.isActive()).thenReturn(true);
        when(aircraftRepo.findByRegistrationNumber(any()))
                .thenReturn(Optional.of(aircraft));

        when(pilotRepo.findByLicenseNumber(PilotId.valueOf("P12345")))
                .thenReturn(Optional.empty());

        final var result = controller.importFlightPlan(VALID_DSL, "test.flightplan", "FP001");

        assertFalse(result.allPassed());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.contains("Pilot not found")),
                "Should contain 'Pilot not found' in semantic errors");
    }

    @Test
    void ensurePilotCompanyMismatchReturnsError() {
        // Route belongs to TP, pilot belongs to FR
        final var route = new FlightRoute(
                FlightRouteName.valueOf("TP999"),
                CompanyIATA.valueOf("TP"),
                AirportIATA.valueOf("LIS"),
                AirportIATA.valueOf("CDG"));
        when(flightRouteRepo.findByOriginAndDestinationAndCompany(any(), any(), any()))
                .thenReturn(Optional.of(route));

        final var aircraft = mock(Aircraft.class);
        when(aircraft.isActive()).thenReturn(true);
        when(aircraftRepo.findByRegistrationNumber(any()))
                .thenReturn(Optional.of(aircraft));

        final var pilot = mock(Pilot.class);
        when(pilot.isActive()).thenReturn(true);
        when(pilot.company()).thenReturn(CompanyIATA.valueOf("FR"));
        when(pilotRepo.findByLicenseNumber(PilotId.valueOf("P12345")))
                .thenReturn(Optional.of(pilot));

        final var result = controller.importFlightPlan(VALID_DSL, "test.flightplan", "FP001");

        assertFalse(result.allPassed());
        assertTrue(result.semanticErrors().stream()
                .anyMatch(e -> e.contains("does not belong to company")),
                "Should contain 'does not belong to company' in semantic errors");
    }

    @Test
    void ensureExtractFlightDesignatorWorks() {
        final var designator = controller.extractFlightDesignator(VALID_DSL);
        assertEquals("TP3000", designator);
    }

    @Test
    void ensureFlightPlansForFlightDelegatesToRepo() {
        final var designator = FlightDesignator.valueOf("TP1234");
        final var flight = mock(Flight.class);
        when(flightRepo.ofIdentity(designator)).thenReturn(java.util.Optional.of(flight));
        when(flight.flightPlans()).thenReturn(List.of());

        final var result = controller.flightPlansForFlight("TP1234");

        assertNotNull(result);
        verify(flightRepo).ofIdentity(designator);
        verify(flight).flightPlans();
    }
}
