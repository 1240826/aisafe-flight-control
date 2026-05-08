package eapli.aisafe.collaborator.application;

import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US064 — Disable Customer's Collaborator (Extra).
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class DisableCollaboratorController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final CollaboratorRepository repo = PersistenceContext.repositories().collaborators();

    public Iterable<Collaborator> activeCollaborators() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.findAllActive();
    }

    /**
     * Disable the collaborator — irreversible.
     */
    public Collaborator disableCollaborator(final Long collaboratorId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final Collaborator collab = repo.ofIdentity(collaboratorId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborator not found: " + collaboratorId));

        collab.disable();
        return repo.save(collab);
    }
}
