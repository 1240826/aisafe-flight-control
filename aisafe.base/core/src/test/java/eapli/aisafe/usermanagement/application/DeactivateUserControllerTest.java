package eapli.aisafe.usermanagement.application;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Controller tests for US032 — Disable/Enable User.
 */
class DeactivateUserControllerTest {

    private AuthorizationService authz;
    private UserManagementService userSvc;
    private DeactivateUserController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        userSvc = mock(UserManagementService.class);
        controller = new DeactivateUserController(authz, userSvc);
    }

    private SystemUser dummyUser(final String username) {
        return new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                .with(username, "Password1", "Test", "User", username + "@aisafe.pt")
                .withRoles(Role.valueOf("BACKOFFICE_OPERATOR"))
                .build();
    }

    // ── Deactivate (US032 AC 032.2) ───────────────────────────────────────────

    @Test
    void ensureDeactivateUserCallsUserService() {
        // Arrange
        final SystemUser user = dummyUser("victim");
        when(userSvc.deactivateUser(user)).thenReturn(user);
        // session returns a different username so self-disable check passes
        when(authz.session()).thenReturn(Optional.empty());

        // Act
        final SystemUser result = controller.deactivateUser(user);

        // Assert
        verify(userSvc).deactivateUser(user);
        assertNotNull(result);
    }

    @Test
    void ensureDeactivateUserChecksAuthorization() {
        // Arrange
        final SystemUser user = dummyUser("victim");
        when(userSvc.deactivateUser(user)).thenReturn(user);
        when(authz.session()).thenReturn(Optional.empty());

        // Act
        controller.deactivateUser(user);

        // Assert — ADMIN and BACKOFFICE_OPERATOR must both be accepted
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    @Test
    void ensureAdminCannotDeactivateOwnAccount() {
        // Arrange — AC 032.5: admin cannot deactivate themselves.
        // UserSession is final so we construct a real one via its public (SystemUser) constructor.
        final SystemUser self = dummyUser("admin1");
        final UserSession realSession = new UserSession(self);
        when(authz.session()).thenReturn(Optional.of(realSession));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.deactivateUser(self),
                "Admin must not be able to deactivate own account");
    }

    // ── Activate (US032 AC 032.3) ─────────────────────────────────────────────

    @Test
    void ensureActivateUserCallsUserService() {
        // Arrange
        final SystemUser user = dummyUser("victim");
        when(userSvc.activateUser(user)).thenReturn(user);

        // Act
        final SystemUser result = controller.activateUser(user);

        // Assert
        verify(userSvc).activateUser(user);
        assertNotNull(result);
    }

    @Test
    void ensureActivateUserChecksAuthorization() {
        // Arrange
        final SystemUser user = dummyUser("victim");
        when(userSvc.activateUser(user)).thenReturn(user);

        // Act
        controller.activateUser(user);

        // Assert
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── List methods ──────────────────────────────────────────────────────────

    @Test
    void ensureActiveUsersDelegatesToUserService() {
        // Arrange
        when(userSvc.activeUsers()).thenReturn(List.of(dummyUser("u1")));

        // Act
        final Iterable<SystemUser> result = controller.activeUsers();

        // Assert
        verify(userSvc).activeUsers();
        assertNotNull(result);
    }

    @Test
    void ensureDeactivatedUsersDelegatesToUserService() {
        // Arrange
        when(userSvc.deactivatedUsers()).thenReturn(List.of(dummyUser("u2")));

        // Act
        final Iterable<SystemUser> result = controller.deactivatedUsers();

        // Assert
        verify(userSvc).deactivatedUsers();
        assertNotNull(result);
    }
}
