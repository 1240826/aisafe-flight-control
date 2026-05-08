package eapli.aisafe.airport.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.airport.domain.AirportICAO;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US052 — Create Airport.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class CreateAirportController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AirportRepository repo = PersistenceContext.repositories().airports();
    private final AirControlAreaRepository acaRepo =
            PersistenceContext.repositories().airControlAreas();

    /**
     * Create and persist a new Airport.
     */
    public Airport createAirport(final String iata, final String icao,
                                  final String name, final String city, final String country,
                                  final double latitude, final double longitude,
                                  final String areaCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final Airport airport = new Airport(
                AirportIATA.valueOf(iata),
                AirportICAO.valueOf(icao),
                name, city, country,
                latitude, longitude,
                AreaCode.valueOf(areaCode));

        return repo.save(airport);
    }

    /** Support method: list ACAs for selection. */
    public Iterable<eapli.aisafe.aircontrolarea.domain.AirControlArea> allAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return acaRepo.findAll();
    }

    /** List all airports. */
    public Iterable<Airport> allAirports() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.ATC_COLLABORATOR, AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return repo.findAll();
    }
}
