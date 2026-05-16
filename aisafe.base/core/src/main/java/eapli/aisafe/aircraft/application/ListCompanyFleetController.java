package eapli.aisafe.aircraft.application;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US072 — List Company Fleet.
 * Actor: Admin / BackOffice Operator / Flight Control Operator / ATC Collaborator.
 */
@UseCaseController
public class ListCompanyFleetController {

    private final AuthorizationService authz;
    private final AircraftRepository aircraftRepo;
    private final AirTransportCompanyRepository companyRepo;

    /** Production constructor — uses framework registries. */
    public ListCompanyFleetController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().airTransportCompanies());
    }

    /** Testing constructor — allows injecting mocks. */
    ListCompanyFleetController(final AuthorizationService authz,
                               final AircraftRepository aircraftRepo,
                               final AirTransportCompanyRepository companyRepo) {
        this.authz = authz;
        this.aircraftRepo = aircraftRepo;
        this.companyRepo = companyRepo;
    }

    /** List the fleet (all aircraft) of a given company. */
    public Iterable<Aircraft> fleetOfCompany(final String companyIata) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return aircraftRepo.findByCompanyId(CompanyIATA.valueOf(companyIata));
    }

    /** List all active aircraft across all companies. */
    public Iterable<Aircraft> allActiveAircraft() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR, AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return aircraftRepo.findAllActive();
    }

    /** Support method: list all companies for selection. */
    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return companyRepo.findAll();
    }
}
