package eapli.aisafe.usermanagement.application;

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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Controller tests for US031 — Register User.
 */
class AddUserControllerTest {

    private AuthorizationService authz;
    private UserManagementService userSvc;
    private UserSecurityProfileRepository profileRepo;
    private AddUserController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        userSvc = mock(UserManagementService.class);
        profileRepo = mock(UserSecurityProfileRepository.class);
        controller = new AddUserController(authz, userSvc, profileRepo);

        // profileRepo.save returns a dummy profile for any call
        when(profileRepo.save(any(UserSecurityProfile.class)))
                .thenReturn(mock(UserSecurityProfile.class));
    }

    private SystemUser dummyUser(final String username) {
        return new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                .with(username, "Password1", "Test", "User", username + "@aisafe.pt")
                .withRoles(Role.valueOf("BACKOFFICE_OPERATOR"))
                .build();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureAddUserRegistersUser() {
        // Arrange
        when(userSvc.registerNewUser(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), any())).thenReturn(dummyUser("newuser"));

        // Act
        final SystemUser result = controller.addUser(
                "newuser", "Password1!", "New", "User", "new@aisafe.pt",
                Set.of(Role.valueOf("BACKOFFICE_OPERATOR")),
                LocalDate.now().plusYears(1));

        // Assert
        assertNotNull(result);
        verify(userSvc).registerNewUser(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void ensureAddUserPersistsSecurityProfile() {
        // Arrange — AC 031.7: clearance expiry must be stored in UserSecurityProfile
        when(userSvc.registerNewUser(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), any())).thenReturn(dummyUser("newuser"));

        // Act
        controller.addUser(
                "newuser", "Password1!", "New", "User", "new@aisafe.pt",
                Set.of(Role.valueOf("BACKOFFICE_OPERATOR")),
                LocalDate.now().plusYears(1));

        // Assert
        verify(profileRepo).save(any(UserSecurityProfile.class));
    }

    @Test
    void ensureAddUserChecksAuthorization() {
        // Arrange — US031: ADMIN, BACKOFFICE_OPERATOR, or POWER_USER must be authorized
        when(userSvc.registerNewUser(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), any())).thenReturn(dummyUser("newuser"));

        // Act
        controller.addUser(
                "newuser", "Password1!", "New", "User", "new@aisafe.pt",
                Set.of(Role.valueOf("BACKOFFICE_OPERATOR")),
                LocalDate.now().plusYears(1));

        // Assert — three roles: ADMIN, BACKOFFICE_OPERATOR, POWER_USER
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any(), any());
    }

    // ── Invalid input ─────────────────────────────────────────────────────────

    @Test
    void ensureAddUserWithPastClearanceDateThrows() {
        // Arrange
        when(userSvc.registerNewUser(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), any())).thenReturn(dummyUser("newuser"));

        // Act & Assert — AC 031.7: clearance date must be today or future
        assertThrows(Exception.class,
                () -> controller.addUser(
                        "newuser", "Password1!", "New", "User", "new@aisafe.pt",
                        Set.of(Role.valueOf("BACKOFFICE_OPERATOR")),
                        LocalDate.now().minusDays(1)),
                "Past security clearance expiry date must be rejected");
    }

    // ── Additional overloads ─────────────────────────────────────────────────

    @Test
    void ensureGetRoleTypesReturnsNonEmpty() {
        final var roles = controller.getRoleTypes();
        assertNotNull(roles);
        assertTrue(roles.length > 0);
    }

    @Test
    void ensureAddUserWithPhoneAndCalendarWorks() {
        when(userSvc.registerNewUser(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), any())).thenReturn(dummyUser("newuser"));

        final SystemUser result = controller.addUser(
                "newuser", "Password1!", "New", "User", "new@aisafe.pt",
                Set.of(Role.valueOf("BACKOFFICE_OPERATOR")),
                LocalDate.now().plusYears(1), "+351912345678",
                java.util.Calendar.getInstance());

        assertNotNull(result);
        verify(userSvc).registerNewUser(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void ensureAddUserWithPhoneConvenienceWorks() {
        when(userSvc.registerNewUser(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), any())).thenReturn(dummyUser("newuser"));

        final SystemUser result = controller.addUser(
                "newuser", "Password1!", "New", "User", "new@aisafe.pt",
                Set.of(Role.valueOf("BACKOFFICE_OPERATOR")),
                LocalDate.now().plusYears(1), "+351912345678");

        assertNotNull(result);
        verify(userSvc).registerNewUser(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }
}
