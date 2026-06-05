package eapli.aisafe.flightroute.application;

import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

import java.time.LocalDate;

/**
 * Controller for US074 — Delete (deactivate) a flight route.
 * Actor: Air Transport Company Collaborator.
 */
@UseCaseController
public class DeleteFlightRouteController {

    private final AuthorizationService authz;
    private final FlightRouteRepository repo;
    private final FlightRepository flightRepo;

    /** Production constructor — uses framework registries. */
    public DeleteFlightRouteController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().flights());
    }

    /** Testing constructor — allows injecting mocks. */
    DeleteFlightRouteController(final AuthorizationService authz,
                                final FlightRouteRepository repo,
                                final FlightRepository flightRepo) {
        this.authz = authz;
        this.repo = repo;
        this.flightRepo = flightRepo;
    }

    /**
     * List all active routes for selection in the UI.
     */
    public Iterable<FlightRoute> activeRoutes() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return repo.findAllActive();
    }

    /**
     * Deactivate a flight route from the given date onwards.
     *
     * @param routeName      name of the route to deactivate (e.g. "TP123")
     * @param deactivationDate date from which no new flights may be created
     * @return the updated FlightRoute
     * @throws IllegalArgumentException if the route does not exist
     * @throws IllegalStateException    if planned flights exist after the given date
     */
    public FlightRoute deactivateRoute(final String routeName, final LocalDate deactivationDate) {
        Preconditions.noneNull(routeName, deactivationDate);
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);

        final FlightRouteName name = FlightRouteName.valueOf(routeName);

        final FlightRoute route = repo.ofIdentity(name)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight route not found: " + routeName));

        if (flightRepo.existsByRouteNameAndDepartureTimeAfter(name,
                deactivationDate.atStartOfDay())) {
            throw new IllegalStateException(
                    "Cannot deactivate route " + routeName
                            + ": planned flights exist on or after " + deactivationDate);
        }

        route.deactivate(deactivationDate);
        return repo.save(route);
    }
}