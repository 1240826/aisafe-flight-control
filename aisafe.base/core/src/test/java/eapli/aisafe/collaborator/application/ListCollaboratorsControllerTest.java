package eapli.aisafe.collaborator.application;

import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.domain.SecurityClearance;
import eapli.aisafe.collaborator.domain.SkillsAssessment;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.domain.CompanyICAO;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListCollaboratorsControllerTest {

    private AuthorizationService authz;
    private CollaboratorRepository collaboratorRepo;
    private AirTransportCompanyRepository companyRepo;
    private ListCollaboratorsController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        collaboratorRepo = mock(CollaboratorRepository.class);
        companyRepo = mock(AirTransportCompanyRepository.class);
        controller = new ListCollaboratorsController(authz, collaboratorRepo, companyRepo);
    }

    private SystemUser dummySystemUser() {
        final SystemUserBuilder b = new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder());
        return b.with("listuser", "Password1", "List", "User", "list@aisafe.pt")
                .withRoles(Role.valueOf("ADMIN"))
                .build();
    }

    private Collaborator makeCollaborator() {
        return Collaborator.ofATC(dummySystemUser(), "Bob Smith", "ATC Junior",
                new SecurityClearance(LocalDate.now().plusYears(1)),
                new SkillsAssessment(LocalDate.now().minusDays(1)),
                CompanyIATA.valueOf("TP"));
    }

    // ── Collaborators of company ──────────────────────────────────────────────

    @Test
    void ensureCollaboratorsOfCompanyDelegatesToRepo() {
        // Arrange
        when(collaboratorRepo.findByCompanyId(CompanyIATA.valueOf("TP")))
                .thenReturn(List.of(makeCollaborator()));

        // Act
        final Iterable<Collaborator> result = controller.collaboratorsOfCompany("TP");

        // Assert
        verify(collaboratorRepo).findByCompanyId(CompanyIATA.valueOf("TP"));
        assertNotNull(result);
    }

    @Test
    void ensureCollaboratorsOfCompanyChecksAuthorization() {
        // Arrange
        when(collaboratorRepo.findByCompanyId(any())).thenReturn(List.of());

        // Act
        controller.collaboratorsOfCompany("TP");

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── All active collaborators ──────────────────────────────────────────────

    @Test
    void ensureAllActiveCollaboratorsDelegatesToRepo() {
        // Arrange
        when(collaboratorRepo.findAllActive()).thenReturn(List.of(makeCollaborator()));

        // Act
        final Iterable<Collaborator> result = controller.allActiveCollaborators();

        // Assert
        verify(collaboratorRepo).findAllActive();
        assertNotNull(result);
    }

    @Test
    void ensureAllActiveCollaboratorsChecksAuthorization() {
        when(collaboratorRepo.findAllActive()).thenReturn(List.of());
        controller.allActiveCollaborators();
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    // ── All companies ─────────────────────────────────────────────────────────

    @Test
    void ensureAllCompaniesDelegatesToRepo() {
        // Arrange
        when(companyRepo.findAll()).thenReturn(List.of(
                new AirTransportCompany(CompanyIATA.valueOf("TP"), CompanyICAO.valueOf("TAP"), "TAP Air Portugal")));

        // Act
        final Iterable<AirTransportCompany> result = controller.allCompanies();

        // Assert
        verify(companyRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllCompaniesChecksAuthorization() {
        // Arrange
        when(companyRepo.findAll()).thenReturn(List.of());

        // Act
        controller.allCompanies();

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }
}
