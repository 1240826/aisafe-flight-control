package eapli.aisafe.usermanagement.application;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.domain.model.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Controller tests for US033 — List Users.
 */
class ListUsersControllerTest {

    private AuthorizationService authz;
    private UserManagementService userSvc;
    private ListUsersController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        userSvc = mock(UserManagementService.class);
        controller = new ListUsersController(authz, userSvc);
    }

    private SystemUser dummyUser(final String username) {
        return new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                .with(username, "Password1", "Test", "User", username + "@aisafe.pt")
                .withRoles(Role.valueOf("BACKOFFICE_OPERATOR"))
                .build();
    }

    // ── All users ─────────────────────────────────────────────────────────────

    @Test
    void ensureAllUsersDelegatesToUserService() {
        // Arrange
        final List<SystemUser> users = List.of(dummyUser("u1"), dummyUser("u2"));
        when(userSvc.activeUsers()).thenReturn(users);

        // Act
        final Iterable<SystemUser> result = controller.allUsers();

        // Assert
        verify(userSvc).activeUsers();
        assertNotNull(result);
    }

    @Test
    void ensureAllUsersOnlyReturnsActiveUsers() {
        // Arrange — US033: disabled users must not appear in the list
        when(userSvc.activeUsers()).thenReturn(List.of(dummyUser("u1")));

        // Act
        controller.allUsers();

        // Assert — must call activeUsers(), NOT allUsers()
        verify(userSvc).activeUsers();
        verify(userSvc, never()).allUsers();
    }

    @Test
    void ensureAllUsersChecksAuthorization() {
        // Arrange — ADMIN and BACKOFFICE_OPERATOR must be allowed
        when(userSvc.activeUsers()).thenReturn(List.of());

        // Act
        controller.allUsers();

        // Assert — two roles: ADMIN, BACKOFFICE_OPERATOR
        verify(authz).ensureAuthenticatedUserHasAnyOf(any(), any());
    }

    // ── Find by username ──────────────────────────────────────────────────────

    @Test
    void ensureFindDelegatesToUserService() {
        // Arrange
        final SystemUser user = dummyUser("u1");
        final Username username = Username.valueOf("u1");
        when(userSvc.userOfIdentity(username)).thenReturn(Optional.of(user));

        // Act
        final Optional<SystemUser> result = controller.find(username);

        // Assert
        verify(userSvc).userOfIdentity(username);
        assertTrue(result.isPresent());
    }

    @Test
    void ensureFindReturnsEmptyWhenNotFound() {
        // Arrange
        final Username username = Username.valueOf("nonexistent");
        when(userSvc.userOfIdentity(username)).thenReturn(Optional.empty());

        // Act
        final Optional<SystemUser> result = controller.find(username);

        // Assert
        assertTrue(result.isEmpty());
    }
}
