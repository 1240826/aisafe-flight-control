package eapli.aisafe.collaborator.application;

import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US062 — List Customer's Collaborators.
 * Actor: Admin / BackOffice Operator / ATC Collaborator.
 */
@UseCaseController
public class ListCollaboratorsController {

    private final AuthorizationService authz;
    private final CollaboratorRepository collaboratorRepo;
    private final AirTransportCompanyRepository companyRepo;

    /** Production constructor — uses framework registries. */
    public ListCollaboratorsController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().collaborators(),
                PersistenceContext.repositories().airTransportCompanies());
    }

    /** Testing constructor — allows injecting mocks. */
    ListCollaboratorsController(final AuthorizationService authz,
                                final CollaboratorRepository collaboratorRepo,
                                final AirTransportCompanyRepository companyRepo) {
        this.authz = authz;
        this.collaboratorRepo = collaboratorRepo;
        this.companyRepo = companyRepo;
    }

    /** List collaborators belonging to a specific company. */
    public Iterable<Collaborator> collaboratorsOfCompany(final String companyIata) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.ATC_COLLABORATOR);
        return collaboratorRepo.findByCompanyId(CompanyIATA.valueOf(companyIata));
    }

    /** List all active collaborators in the system. */
    public Iterable<Collaborator> allActiveCollaborators() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return collaboratorRepo.findAllActive();
    }

    /** List all companies to allow selection. */
    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.ATC_COLLABORATOR);
        return companyRepo.findAll();
    }
}
