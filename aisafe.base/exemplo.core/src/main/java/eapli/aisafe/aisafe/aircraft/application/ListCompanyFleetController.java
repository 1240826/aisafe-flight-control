package eapli.aisafe.aircraft.application;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US072 — List Company Fleet.
 * Actor: Admin / BackOffice Operator / Flight Control Operator / ATC Collaborator.
 */
@UseCaseController
public class ListCompanyFleetController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepo = PersistenceContext.repositories().aircraft();
    private final AirTransportCompanyRepository companyRepo =
            PersistenceContext.repositories().airTransportCompanies();

    /** List the fleet (all aircraft) of a given company. */
    public Iterable<Aircraft> fleetOfCompany(final String companyIata) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR, AISafeRoles.ATC_COLLABORATOR);
        return aircraftRepo.findByCompanyId(CompanyIATA.valueOf(companyIata));
    }

    /** List all active aircraft across all companies. */
    public Iterable<Aircraft> allActiveAircraft() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return aircraftRepo.findAllActive();
    }

    /** Support method: list all companies for selection. */
    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR, AISafeRoles.ATC_COLLABORATOR);
        return companyRepo.findAll();
    }
}
