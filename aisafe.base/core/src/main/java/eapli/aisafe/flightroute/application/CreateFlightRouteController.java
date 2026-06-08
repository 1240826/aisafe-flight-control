package eapli.aisafe.flightroute.application;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.aisafe.flightroute.domain.FlightRoute;

import java.util.Optional;

@UseCaseController
public class CreateFlightRouteController {

    private final AuthorizationService authz;
    private final FlightRouteRepository routeRepo;
    private final AirportRepository airportRepo;
    private final AirTransportCompanyRepository companyRepo;

    public CreateFlightRouteController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().airports(),
                PersistenceContext.repositories().airTransportCompanies());
    }

    CreateFlightRouteController(final AuthorizationService authz,
                                final FlightRouteRepository routeRepo,
                                final AirportRepository airportRepo,
                                final AirTransportCompanyRepository companyRepo) {
        this.authz = authz;
        this.routeRepo = routeRepo;
        this.airportRepo = airportRepo;
        this.companyRepo = companyRepo;
    }

    public Iterable<Airport> allAirports() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return airportRepo.findAll();
    }

    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return companyRepo.findAll();
    }

    public boolean routeExists(final String routeName) {
        return routeRepo.ofIdentity(FlightRouteName.valueOf(routeName)).isPresent();
    }

    public Iterable<FlightRoute> allActiveRoutes() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return routeRepo.findAllActive();
    }

    /**
     * US073: create a new active flight route.
     * @throws IllegalArgumentException if the route already exists (even if deactivated)
     */
    public FlightRoute createFlightRoute(final String routeName,
                                          final String companyIata,
                                          final String originCode,
                                          final String destinationCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);

        final var name = FlightRouteName.valueOf(routeName);
        final var company = CompanyIATA.valueOf(companyIata);
        final var origin = AirportIATA.valueOf(originCode);
        final var destination = AirportIATA.valueOf(destinationCode);

        final var existing = routeRepo.ofIdentity(name);
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    "Route '" + routeName + "' already exists.");
        }

        final var route = new FlightRoute(name, company, origin, destination);
        return routeRepo.save(route);
    }
}
