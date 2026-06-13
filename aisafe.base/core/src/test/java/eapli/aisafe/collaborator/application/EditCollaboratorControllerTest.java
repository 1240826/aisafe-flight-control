package eapli.aisafe.collaborator.application;

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

class EditCollaboratorControllerTest {

    private AuthorizationService authz;
    private CollaboratorRepository repo;
    private EditCollaboratorController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        repo = mock(CollaboratorRepository.class);
        controller = new EditCollaboratorController(authz, repo);
    }

    private SystemUser dummySystemUser() {
        final SystemUserBuilder b = new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder());
        return b.with("edituser", "Password1", "Edit", "User", "edit@aisafe.pt")
                .withRoles(Role.valueOf("ADMIN"))
                .build();
    }

    private Collaborator makeCollaborator() {
        return Collaborator.ofATC(dummySystemUser(), "Alice Smith", "ATC Officer",
                new SecurityClearance(LocalDate.now().plusYears(1)),
                new SkillsAssessment(LocalDate.now().minusDays(1)),
                CompanyIATA.valueOf("TP"));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureEditCollaboratorUpdatesNameAndSaves() {
        // Arrange
        final Collaborator collab = makeCollaborator();
        when(repo.ofIdentity(1L)).thenReturn(Optional.of(collab));
        when(repo.save(collab)).thenReturn(collab);

        // Act — phone=null means no phone change
        final Collaborator result = controller.editCollaborator(1L, "Alice Johnson", null, null, null, null);

        // Assert
        assertEquals("Alice Johnson", result.name());
        verify(repo).save(collab);
    }

    @Test
    void ensureEditCollaboratorUpdatesPositionAndSaves() {
        // Arrange
        final Collaborator collab = makeCollaborator();
        when(repo.ofIdentity(1L)).thenReturn(Optional.of(collab));
        when(repo.save(collab)).thenReturn(collab);

        // Act
        final Collaborator result = controller.editCollaborator(1L, null, "Senior ATC", null, null, null);

        // Assert
        assertEquals("Senior ATC", result.position());
        verify(repo).save(collab);
    }

    @Test
    void ensureEditCollaboratorUpdatesPhoneAndSaves() {
        // Arrange
        final Collaborator collab = makeCollaborator();
        when(repo.ofIdentity(1L)).thenReturn(Optional.of(collab));
        when(repo.save(collab)).thenReturn(collab);

        // Act
        final Collaborator result = controller.editCollaborator(1L, null, null, "+351912345678", null, null);

        // Assert
        assertEquals("+351912345678", result.phone());
        verify(repo).save(collab);
    }

    @Test
    void ensureEditCollaboratorUpdatesClearanceAndSaves() {
        // Arrange
        final Collaborator collab = makeCollaborator();
        when(repo.ofIdentity(1L)).thenReturn(Optional.of(collab));
        when(repo.save(collab)).thenReturn(collab);
        final LocalDate newExpiry = LocalDate.now().plusYears(3);

        // Act
        final Collaborator result = controller.editCollaborator(1L, null, null, null, newExpiry, null);

        // Assert
        assertEquals(newExpiry, result.securityClearance().expiryDate());
        verify(repo).save(collab);
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    void ensureEditCollaboratorNotFoundThrowsException() {
        // Arrange
        when(repo.ofIdentity(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.editCollaborator(999L, "New Name", null, null, null, null),
                "Must throw IllegalArgumentException when collaborator is not found");
    }

    // ── Auth check ────────────────────────────────────────────────────────────

    @Test
    void ensureEditCollaboratorChecksAuthorization() {
        // Arrange
        final Collaborator collab = makeCollaborator();
        when(repo.ofIdentity(1L)).thenReturn(Optional.of(collab));
        when(repo.save(collab)).thenReturn(collab);

        // Act
        controller.editCollaborator(1L, "New Name", null, null, null, null);

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Test
    void ensureActiveCollaboratorsDelegatesToRepo() {
        // Arrange
        when(repo.findAllActive()).thenReturn(List.of(makeCollaborator()));

        // Act
        final Iterable<Collaborator> result = controller.activeCollaborators();

        // Assert
        verify(repo).findAllActive();
        assertNotNull(result);
    }

    @Test
    void ensureEditCollaboratorUpdatesAssessmentAndSaves() {
        final Collaborator collab = makeCollaborator();
        when(repo.ofIdentity(1L)).thenReturn(Optional.of(collab));
        when(repo.save(collab)).thenReturn(collab);
        final LocalDate newAssessment = LocalDate.now().minusDays(5);

        final Collaborator result = controller.editCollaborator(1L, null, null, null, null, newAssessment);

        assertEquals(newAssessment, result.skillsAssessment().assessmentDate());
        verify(repo).save(collab);
    }

    @Test
    void ensureActiveCollaboratorsChecksAuthorization() {
        when(repo.findAllActive()).thenReturn(List.of());
        controller.activeCollaborators();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }
}
