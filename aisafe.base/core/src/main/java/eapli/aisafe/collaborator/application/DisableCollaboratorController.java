package eapli.aisafe.collaborator.application;

import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US064 — Disable Customer's Collaborator (Extra).
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class DisableCollaboratorController {

    private final AuthorizationService authz;
    private final CollaboratorRepository repo;

    /** Production constructor — uses framework registries. */
    public DisableCollaboratorController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().collaborators());
    }

    /** Testing constructor — allows injecting mocks. */
    DisableCollaboratorController(final AuthorizationService authz, final CollaboratorRepository repo) {
        this.authz = authz;
        this.repo = repo;
    }

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
