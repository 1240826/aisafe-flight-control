package eapli.aisafe.flightroute.application;

import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListFlightRoutesControllerTest {

    private AuthorizationService authz;
    private FlightRouteRepository flightRouteRepo;
    private ListFlightRoutesController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        flightRouteRepo = mock(FlightRouteRepository.class);
        controller = new ListFlightRoutesController(authz, flightRouteRepo);
    }

    @Test
    void ensureAllRoutesDelegatesToRepo() {
        when(flightRouteRepo.findAll()).thenReturn(List.of());
        final Iterable<FlightRoute> result = controller.allRoutes();
        verify(flightRouteRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllRoutesChecksAuthorization() {
        when(flightRouteRepo.findAll()).thenReturn(List.of());
        controller.allRoutes();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    @Test
    void ensureAllRoutesReturnsRepoResults() {
        final FlightRoute route = mock(FlightRoute.class);
        when(flightRouteRepo.findAll()).thenReturn(List.of(route));
        final Iterable<FlightRoute> result = controller.allRoutes();
        assertTrue(result.iterator().hasNext());
    }

    @Test
    void ensureAllRoutesReturnsEmptyWhenNoRoutes() {
        when(flightRouteRepo.findAll()).thenReturn(List.of());
        final Iterable<FlightRoute> result = controller.allRoutes();
        assertFalse(result.iterator().hasNext());
    }
}
