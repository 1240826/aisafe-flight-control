package eapli.aisafe.pilot.application;

import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * US076: List company pilot roster.
 * Base implementation — UI by responsible colleague.
 */
@UseCaseController
public class ListPilotRosterController {

    private final AuthorizationService authz;
    private final PilotRepository pilotRepo;
    private final AirTransportCompanyRepository companyRepo;

    public ListPilotRosterController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().airTransportCompanies());
    }

    ListPilotRosterController(final AuthorizationService authz,
                                final PilotRepository pilotRepo,
                                final AirTransportCompanyRepository companyRepo) {
        this.authz = authz;
        this.pilotRepo = pilotRepo;
        this.companyRepo = companyRepo;
    }

    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return companyRepo.findAll();
    }

    public Iterable<Pilot> listCompanyPilots(final CompanyIATA company) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return pilotRepo.findByCompany(company);
    }

    public Iterable<Pilot> listActiveCompanyPilots(final CompanyIATA company) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ATC_COLLABORATOR);
        return pilotRepo.findActiveByCompany(company);
    }
}
