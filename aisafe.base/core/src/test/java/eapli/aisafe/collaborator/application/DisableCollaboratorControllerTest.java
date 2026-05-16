package eapli.aisafe.collaborator.application;

import eapli.aisafe.collaborator.domain.ATCCollaborator;
import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.domain.SecurityClearance;
import eapli.aisafe.collaborator.domain.SkillsAssessment;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisableCollaboratorControllerTest {

    private AuthorizationService authz;
    private CollaboratorRepository repo;
    private DisableCollaboratorController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(CollaboratorRepository.class);
        controller = new DisableCollaboratorController(authz, repo);
    }

    private SystemUser dummySystemUser() {
        final SystemUserBuilder b = new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder());
        return b.with("testuser", "Password1", "Test", "User", "test@aisafe.pt")
                .withRoles(Role.valueOf("ADMIN"))
                .build();
    }

    private ATCCollaborator makeCollaborator(final SystemUser su) {
        return new ATCCollaborator(su, "John Doe", "Senior ATC",
                new SecurityClearance(LocalDate.now().plusYears(1)),
                new SkillsAssessment(LocalDate.now().minusDays(1)),
                CompanyIATA.valueOf("TP"));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureDisableCollaboratorCallsDisable() {
        // Arrange
        final ATCCollaborator collab = makeCollaborator(dummySystemUser());
        when(repo.ofIdentity(1L)).thenReturn(Optional.of(collab));
        when(repo.save(collab)).thenReturn(collab);

        // Act
        final Collaborator result = controller.disableCollaborator(1L);

        // Assert
        assertFalse(result.isActive(), "Collaborator must be inactive after disabling");
        verify(repo).save(collab);
    }

    @Test
    void ensureDisableCollaboratorChecksAuthorization() {
        // Arrange
        final ATCCollaborator collab = makeCollaborator(dummySystemUser());
        when(repo.ofIdentity(1L)).thenReturn(Optional.of(collab));
        when(repo.save(collab)).thenReturn(collab);

        // Act
        controller.disableCollaborator(1L);

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void ensureDisableCollaboratorNotFoundThrowsException() {
        // Arrange
        when(repo.ofIdentity(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.disableCollaborator(999L),
                "Must throw when collaborator is not found");
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Test
    void ensureActiveCollaboratorsReturnsListFromRepo() {
        // Arrange
        final ATCCollaborator collab = makeCollaborator(dummySystemUser());
        when(repo.findAllActive()).thenReturn(List.of(collab));

        // Act
        final Iterable<Collaborator> result = controller.activeCollaborators();

        // Assert
        verify(repo).findAllActive();
        assertNotNull(result);
    }

    @Test
    void ensureDisablingAlreadyDisabledThrows() {
        // Arrange
        final ATCCollaborator collab = makeCollaborator(dummySystemUser());
        collab.disable(); // pre-disable
        when(repo.ofIdentity(1L)).thenReturn(Optional.of(collab));

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> controller.disableCollaborator(1L),
                "Disabling an already-disabled collaborator must throw IllegalStateException");
    }
}
