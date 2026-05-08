package eapli.aisafe.collaborator.application;

import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US062 — List Customer's Collaborators.
 * Actor: Admin / BackOffice Operator / ATC Collaborator.
 */
@UseCaseController
public class ListCollaboratorsController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final CollaboratorRepository collaboratorRepo =
            PersistenceContext.repositories().collaborators();
    private final AirTransportCompanyRepository companyRepo =
            PersistenceContext.repositories().airTransportCompanies();

    /** List collaborators belonging to a specific company. */
    public Iterable<Collaborator> collaboratorsOfCompany(final String companyIata) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.ATC_COLLABORATOR);
        return collaboratorRepo.findByCompanyId(CompanyIATA.valueOf(companyIata));
    }

    /** List all active collaborators in the system. */
    public Iterable<Collaborator> allActiveCollaborators() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return collaboratorRepo.findAllActive();
    }

    /** List all companies to allow selection. */
    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.ATC_COLLABORATOR);
        return companyRepo.findAll();
    }
}
