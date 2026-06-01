package eapli.aisafe.flightroute.application;

import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeleteFlightRouteController.
 * Covers US074: Delete (deactivate) a flight route.
 *
 * NOTE: DeleteFlightRouteController does not yet exist — these tests drive its implementation.
 * Expected behaviour:
 *   - Finds the route by name.
 *   - Checks that no planned flights exist after the requested date (via repo).
 *   - Delegates deactivation to the domain and persists.
 *   - Rejects deactivation if planned flights exist after the date.
 *   - Enforces authorization.
 */
class DeleteFlightRouteControllerTest {

    private AuthorizationService authz;
    private FlightRouteRepository routeRepo;
    private DeleteFlightRouteController controller;

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(30);

    @BeforeEach
    void setUp() {
        authz     = mock(AuthorizationService.class);
        routeRepo = mock(FlightRouteRepository.class);
        controller = new DeleteFlightRouteController(authz, routeRepo);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private FlightRoute activeRoute() {
        return new FlightRoute(
                FlightRouteName.valueOf("TP123"),
                CompanyIATA.valueOf("TP"),
                AirportIATA.valueOf("OPO"),
                AirportIATA.valueOf("LIS")
        );
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureDeactivateRouteWithNoFuturePlannedFlightsSucceeds() {
        // AT1: no planned flights after date → deactivation persisted
        final var route = activeRoute();
        when(routeRepo.ofIdentity(FlightRouteName.valueOf("TP123"))).thenReturn(Optional.of(route));
        when(routeRepo.hasPlannedFlightsAfter(FlightRouteName.valueOf("TP123"), FUTURE_DATE)).thenReturn(false);
        when(routeRepo.save(route)).thenReturn(route);

        final var result = controller.deactivateRoute("TP123", FUTURE_DATE);

        assertFalse(result.isActive(), "Route must be inactive after deactivation");
        verify(routeRepo).save(route);
    }

    @Test
    void ensureDeactivateRouteChecksAuthorization() {
        // AT1/AT5: authorization must always be checked
        final var route = activeRoute();
        when(routeRepo.ofIdentity(FlightRouteName.valueOf("TP123"))).thenReturn(Optional.of(route));
        when(routeRepo.hasPlannedFlightsAfter(any(), any())).thenReturn(false);
        when(routeRepo.save(route)).thenReturn(route);

        controller.deactivateRoute("TP123", FUTURE_DATE);

        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureActiveRoutesChecksAuthorization() {
        // Authorization enforced for listing too
        when(routeRepo.findAllActive()).thenReturn(List.of());
        controller.activeRoutes();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureActiveRoutesReturnsRepoResults() {
        // Support method must delegate to repository
        final var route = activeRoute();
        when(routeRepo.findAllActive()).thenReturn(List.of(route));

        final var result = controller.activeRoutes();

        assertNotNull(result);
        verify(routeRepo).findAllActive();
    }

    // ── Business rule: planned flights block deactivation ─────────────────────

    @Test
    void ensureDeactivateRouteWithFuturePlannedFlightsThrows() {
        // AT2: planned flights after the date → operation must be rejected
        final var route = activeRoute();
        when(routeRepo.ofIdentity(FlightRouteName.valueOf("TP123"))).thenReturn(Optional.of(route));
        when(routeRepo.hasPlannedFlightsAfter(FlightRouteName.valueOf("TP123"), FUTURE_DATE)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> controller.deactivateRoute("TP123", FUTURE_DATE),
                "Deactivation must be rejected when planned flights exist after the date");
    }

    @Test
    void ensureDeactivatedRouteIsNotSavedWhenFlightsExist() {
        // AT2: repo.save must NOT be called if business rule blocks deactivation
        final var route = activeRoute();
        when(routeRepo.ofIdentity(FlightRouteName.valueOf("TP123"))).thenReturn(Optional.of(route));
        when(routeRepo.hasPlannedFlightsAfter(any(), any())).thenReturn(true);

        try {
            controller.deactivateRoute("TP123", FUTURE_DATE);
        } catch (IllegalStateException ignored) {
            // expected
        }

        verify(routeRepo, never()).save(any());
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void ensureDeactivateNonExistentRouteThrows() {
        // AT4: route not found → fail clearly
        when(routeRepo.ofIdentity(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> controller.deactivateRoute("XX999", FUTURE_DATE),
                "deactivateRoute must throw IllegalArgumentException when route is not found");
    }

    // ── Guard clauses ─────────────────────────────────────────────────────────

    @Test
    void ensureNullRouteNameThrows() {
        // AT4: route name is mandatory
        assertThrows(Exception.class,
                () -> controller.deactivateRoute(null, FUTURE_DATE),
                "deactivateRoute must reject a null route name");
    }

    @Test
    void ensureNullDeactivationDateThrows() {
        // AT4: date is mandatory
        assertThrows(Exception.class,
                () -> controller.deactivateRoute("TP123", null),
                "deactivateRoute must reject a null deactivation date");
    }
}
