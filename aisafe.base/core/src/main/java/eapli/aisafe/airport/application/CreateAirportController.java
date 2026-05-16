package eapli.aisafe.airport.application;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.airport.domain.AirportICAO;
import eapli.aisafe.airport.domain.Elevation;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US052 — Create Airport.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class CreateAirportController {

    private final AuthorizationService authz;
    private final AirportRepository repo;
    private final AirControlAreaRepository acaRepo;

    /** Production constructor — uses framework registries. */
    public CreateAirportController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().airports(),
                PersistenceContext.repositories().airControlAreas());
    }

    /** Testing constructor — allows injecting mocks. */
    CreateAirportController(final AuthorizationService authz,
                            final AirportRepository repo,
                            final AirControlAreaRepository acaRepo) {
        this.authz = authz;
        this.repo = repo;
        this.acaRepo = acaRepo;
    }

    /**
     * Create and persist a new Airport.
     *
     * @param elevationValue positive value in elevationUnit (US052.6)
     * @param elevationUnit  unit string, e.g. "m" or "ft" (US052.6)
     */
    public Airport createAirport(final String iata, final String icao,
                                  final String name, final String city, final String country,
                                  final double latitude, final double longitude,
                                  final double elevationValue, final String elevationUnit,
                                  final String areaCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final AirControlArea aca = acaRepo.ofIdentity(AreaCode.valueOf(areaCode))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Air Control Area '" + areaCode + "' does not exist."));

        if (!aca.containsCoordinates(latitude, longitude)) {
            throw new IllegalArgumentException(
                    "Airport coordinates (" + latitude + ", " + longitude
                    + ") are outside the ACA '" + areaCode + "' boundary.");
        }

        final Airport airport = new Airport(
                AirportIATA.valueOf(iata),
                AirportICAO.valueOf(icao),
                name, city, country,
                latitude, longitude,
                new Elevation(elevationValue, elevationUnit),
                AreaCode.valueOf(areaCode));

        return repo.save(airport);
    }

    /** Support method: list ACAs for selection. */
    public Iterable<eapli.aisafe.aircontrolarea.domain.AirControlArea> allAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return acaRepo.findAll();
    }

    /** List all airports. */
    public Iterable<Airport> allAirports() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.ATC_COLLABORATOR, AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return repo.findAll();
    }
}
