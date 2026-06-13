package eapli.aisafe.flightroute.application;

import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateFlightRouteControllerTest {

    private AuthorizationService authz;
    private FlightRouteRepository routeRepo;
    private AirportRepository airportRepo;
    private AirTransportCompanyRepository companyRepo;
    private CreateFlightRouteController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        routeRepo = mock(FlightRouteRepository.class);
        airportRepo = mock(AirportRepository.class);
        companyRepo = mock(AirTransportCompanyRepository.class);
        controller = new CreateFlightRouteController(authz, routeRepo, airportRepo, companyRepo);
    }

    @Test
    void ensureCreateFlightRouteSavesRoute() {
        when(routeRepo.ofIdentity(FlightRouteName.valueOf("TP123"))).thenReturn(Optional.empty());
        when(routeRepo.save(any(FlightRoute.class))).thenReturn(mock(FlightRoute.class));

        final FlightRoute result = controller.createFlightRoute("TP123", "TP", "OPO", "LIS");

        assertNotNull(result);
        verify(routeRepo).save(any(FlightRoute.class));
    }

    @Test
    void ensureCreateFlightRouteRejectsExistingRoute() {
        final FlightRoute existing = mock(FlightRoute.class);
        when(routeRepo.ofIdentity(FlightRouteName.valueOf("TP123"))).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> controller.createFlightRoute("TP123", "TP", "OPO", "LIS"),
                "Should reject creation of an already existing route");
    }

    @Test
    void ensureCreateFlightRouteChecksAuthorization() {
        when(routeRepo.ofIdentity(any())).thenReturn(Optional.empty());
        when(routeRepo.save(any())).thenReturn(mock(FlightRoute.class));

        controller.createFlightRoute("TP123", "TP", "OPO", "LIS");

        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── Support methods ─────────────────────────────────────────────────────

    @Test
    void ensureAllAirportsDelegatesToRepo() {
        when(airportRepo.findAll()).thenReturn(List.of());
        final var result = controller.allAirports();
        verify(airportRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllCompaniesDelegatesToRepo() {
        when(companyRepo.findAll()).thenReturn(List.of());
        final var result = controller.allCompanies();
        verify(companyRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureRouteExistsReturnsTrueWhenFound() {
        when(routeRepo.ofIdentity(FlightRouteName.valueOf("TP123"))).thenReturn(Optional.of(mock(FlightRoute.class)));
        assertTrue(controller.routeExists("TP123"));
    }

    @Test
    void ensureRouteExistsReturnsFalseWhenNotFound() {
        when(routeRepo.ofIdentity(FlightRouteName.valueOf("XY999"))).thenReturn(Optional.empty());
        assertFalse(controller.routeExists("XY999"));
    }

    @Test
    void ensureAllActiveRoutesDelegatesToRepo() {
        when(routeRepo.findAllActive()).thenReturn(List.of());
        final var result = controller.allActiveRoutes();
        verify(routeRepo).findAllActive();
        assertNotNull(result);
    }
}
