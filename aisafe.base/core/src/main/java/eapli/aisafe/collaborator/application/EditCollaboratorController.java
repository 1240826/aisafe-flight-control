package eapli.aisafe.collaborator.application;

import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.domain.SecurityClearance;
import eapli.aisafe.collaborator.domain.SkillsAssessment;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
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

    private final AuthorizationService authz;
    private final CollaboratorRepository repo;

    /** Production constructor — uses framework registries. */
    public EditCollaboratorController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().collaborators());
    }

    /** Testing constructor — allows injecting mocks. */
    EditCollaboratorController(final AuthorizationService authz, final CollaboratorRepository repo) {
        this.authz = authz;
        this.repo = repo;
    }

    public Iterable<Collaborator> activeCollaborators() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.findAllActive();
    }

    /**
     * Update name, position, phone, security clearance and skills assessment of a collaborator.
     * US063: email and phone must be editable.
     */
    public Collaborator editCollaborator(final Long collaboratorId,
                                          final String newName, final String newPosition,
                                          final String newPhone,
                                          final LocalDate newClearanceExpiry,
                                          final LocalDate newAssessmentDate) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final Collaborator collab = repo.ofIdentity(collaboratorId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborator not found: " + collaboratorId));

        if (newName != null && !newName.isBlank()) {
            collab.updateName(newName);
        }
        if (newPosition != null && !newPosition.isBlank()) {
            collab.updatePosition(newPosition);
        }
        // phone may be set to null/blank to clear it
        collab.updatePhone(newPhone);
        if (newClearanceExpiry != null) {
            collab.renewSecurityClearance(new SecurityClearance(newClearanceExpiry));
        }
        if (newAssessmentDate != null) {
            collab.updateSkillsAssessment(new SkillsAssessment(newAssessmentDate));
        }

        return repo.save(collab);
    }
}
