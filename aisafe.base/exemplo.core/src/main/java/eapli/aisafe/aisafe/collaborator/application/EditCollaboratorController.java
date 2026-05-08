package eapli.aisafe.collaborator.application;

import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.domain.SecurityClearance;
import eapli.aisafe.collaborator.domain.SkillsAssessment;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.time.LocalDate;

/**
 * Controller for US063 — Edit Customer's Collaborator (Extra).
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class EditCollaboratorController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final CollaboratorRepository repo = PersistenceContext.repositories().collaborators();

    public Iterable<Collaborator> activeCollaborators() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.findAllActive();
    }

    /**
     * Update name, position, security clearance and skills assessment of a collaborator.
     */
    public Collaborator editCollaborator(final Long collaboratorId,
                                          final String newName, final String newPosition,
                                          final LocalDate newClearanceExpiry,
                                          final LocalDate newAssessmentDate) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final Collaborator collab = repo.ofIdentity(collaboratorId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborator not found: " + collaboratorId));

        if (newName != null && !newName.isBlank()) {
            collab.updateName(newName);
        }
        if (newPosition != null && !newPosition.isBlank()) {
            collab.updatePosition(newPosition);
        }
        if (newClearanceExpiry != null) {
            collab.renewSecurityClearance(new SecurityClearance(newClearanceExpiry));
        }
        if (newAssessmentDate != null) {
            collab.updateSkillsAssessment(new SkillsAssessment(newAssessmentDate));
        }

        return repo.save(collab);
    }
}
