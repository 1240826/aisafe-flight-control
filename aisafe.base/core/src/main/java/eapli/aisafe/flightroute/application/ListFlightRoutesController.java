package eapli.aisafe.flightroute.application;

import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

@UseCaseController
public class ListFlightRoutesController {

    private final AuthorizationService authz;
    private final FlightRouteRepository flightRouteRepo;

    public ListFlightRoutesController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().flightRoutes());
    }

    ListFlightRoutesController(final AuthorizationService authz,
                                final FlightRouteRepository flightRouteRepo) {
        this.authz = authz;
        this.flightRouteRepo = flightRouteRepo;
    }

    public Iterable<FlightRoute> allRoutes() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR,
                AISafeRoles.ATC_COLLABORATOR);
        return flightRouteRepo.findAll();
    }
}
