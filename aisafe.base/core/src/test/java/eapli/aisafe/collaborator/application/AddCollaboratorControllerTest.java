package eapli.aisafe.collaborator.application;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.collaborator.domain.ATCCollaborator;
import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.domain.SecurityClearance;
import eapli.aisafe.collaborator.domain.SkillsAssessment;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.domain.CompanyICAO;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.usermanagement.domain.UserSecurityProfile;
import eapli.aisafe.usermanagement.repositories.UserSecurityProfileRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.UserManagementService;
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

class AddCollaboratorControllerTest {

    private AuthorizationService authz;
    private UserManagementService userSvc;
    private CollaboratorRepository collaboratorRepo;
    private AirTransportCompanyRepository companyRepo;
    private AirControlAreaRepository acaRepo;
    private UserSecurityProfileRepository profileRepo;
    private AddCollaboratorController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        userSvc = mock(UserManagementService.class);
        collaboratorRepo = mock(CollaboratorRepository.class);
        companyRepo = mock(AirTransportCompanyRepository.class);
        acaRepo = mock(AirControlAreaRepository.class);
        profileRepo = mock(UserSecurityProfileRepository.class);
        controller = new AddCollaboratorController(authz, userSvc, collaboratorRepo,
                companyRepo, acaRepo, profileRepo);

        // profileRepo.save() returns a dummy profile for any call
        when(profileRepo.save(any(UserSecurityProfile.class)))
                .thenReturn(mock(UserSecurityProfile.class));
    }

    private SystemUser dummySystemUser() {
        final SystemUserBuilder b = new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder());
        return b.with("adduser", "Password1", "Add", "User", "add@aisafe.pt")
                .withRoles(Role.valueOf("ATC_COLLABORATOR"))
                .build();
    }

    private ATCCollaborator makeATCCollaborator(final SystemUser su) {
        return new ATCCollaborator(su, "Jane Doe", "ATC Officer",
                new SecurityClearance(LocalDate.now().plusYears(1)),
                new SkillsAssessment(LocalDate.now().minusDays(1)),
                CompanyIATA.valueOf("TP"));
    }

    // ── Add ATC Collaborator ──────────────────────────────────────────────────

    @Test
    void ensureAddATCCollaboratorSavesCollaborator() {
        // Arrange
        final SystemUser mockUser = mock(SystemUser.class);
        when(userSvc.registerNewUser(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(mockUser);
        final ATCCollaborator expected = makeATCCollaborator(dummySystemUser());
        when(collaboratorRepo.save(any(Collaborator.class))).thenReturn(expected);

        // Act
        final Collaborator result = controller.addATCCollaborator(
                "jdoe", "Pass1234!", "Jane", "Doe", "jdoe@aisafe.pt",
                "Jane Doe", "ATC Officer",
                LocalDate.now().plusYears(1),
                LocalDate.now().minusDays(1),
                "TP");

        // Assert
        verify(collaboratorRepo).save(any(Collaborator.class));
        assertNotNull(result);
    }

    @Test
    void ensureAddATCCollaboratorCreatesSecurityProfile() {
        // Arrange — AC 031.7-equivalent: UserSecurityProfile must be saved
        final SystemUser mockUser = mock(SystemUser.class);
        when(userSvc.registerNewUser(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(mockUser);
        when(collaboratorRepo.save(any())).thenReturn(mock(Collaborator.class));

        // Act
        controller.addATCCollaborator(
                "jdoe", "Pass1234!", "Jane", "Doe", "jdoe@aisafe.pt",
                "Jane Doe", "ATC Officer",
                LocalDate.now().plusYears(1),
                LocalDate.now().minusDays(1),
                "TP");

        // Assert
        verify(profileRepo).save(any(UserSecurityProfile.class));
    }

    @Test
    void ensureAddATCCollaboratorChecksAuthorization() {
        // Arrange
        final SystemUser mockUser = mock(SystemUser.class);
        when(userSvc.registerNewUser(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(mockUser);
        when(collaboratorRepo.save(any())).thenReturn(mock(Collaborator.class));

        // Act
        controller.addATCCollaborator(
                "jdoe", "Pass1234!", "Jane", "Doe", "jdoe@aisafe.pt",
                "Jane Doe", "ATC Officer",
                LocalDate.now().plusYears(1),
                LocalDate.now().minusDays(1),
                "TP");

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any());
    }

    @Test
    void ensureAddFlightControlOperatorSavesCollaborator() {
        // Arrange
        final SystemUser mockUser = mock(SystemUser.class);
        when(userSvc.registerNewUser(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(mockUser);
        when(collaboratorRepo.save(any(Collaborator.class))).thenReturn(mock(Collaborator.class));

        // Act
        final Collaborator result = controller.addFlightControlOperator(
                "fcouser", "Pass1234!", "Flight", "Control", "fco@aisafe.pt",
                "FCO Name", "Senior FCO",
                LocalDate.now().plusYears(1),
                LocalDate.now().minusDays(1),
                "LPPC");

        // Assert
        verify(collaboratorRepo).save(any(Collaborator.class));
        assertNotNull(result);
    }

    @Test
    void ensureAddWeatherPersonSavesCollaborator() {
        // Arrange
        final SystemUser mockUser = mock(SystemUser.class);
        when(userSvc.registerNewUser(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(mockUser);
        when(collaboratorRepo.save(any(Collaborator.class))).thenReturn(mock(Collaborator.class));

        // Act
        final Collaborator result = controller.addWeatherPerson(
                "wpuser", "Pass1234!", "Weather", "Person", "wp@aisafe.pt",
                "WP Name", "Meteorologist",
                LocalDate.now().plusYears(1),
                LocalDate.now().minusDays(1),
                "LPPC");

        // Assert
        verify(collaboratorRepo).save(any(Collaborator.class));
        assertNotNull(result);
    }

    // ── Support methods ───────────────────────────────────────────────────────

    @Test
    void ensureAllCompaniesDelegatesToRepo() {
        // Arrange
        when(companyRepo.findAll()).thenReturn(List.of(
                new AirTransportCompany(CompanyIATA.valueOf("TP"), CompanyICAO.valueOf("TAP"), "TAP")));

        // Act
        final Iterable<AirTransportCompany> result = controller.allCompanies();

        // Assert
        verify(companyRepo).findAll();
        assertNotNull(result);
    }

    @Test
    void ensureAllAirControlAreasDelegatesToRepo() {
        // Arrange
        when(acaRepo.findAll()).thenReturn(List.of());

        // Act
        final Iterable<AirControlArea> result = controller.allAirControlAreas();

        // Assert
        verify(acaRepo).findAll();
        assertNotNull(result);
    }
}
