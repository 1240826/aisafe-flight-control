/*
 * Copyright (c) 2013-2024 the original author or authors.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package eapli.aisafe.usermanagement.application;

import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

/**
 * US032 — Disable/Enable Users.
 * ADMIN role required (AC 032.1).
 * Disable prevents login (AC 032.2); Enable restores it (AC 032.3).
 * Does NOT cascade to Collaborator aggregate (AC 032.4).
 */
@UseCaseController
public class DeactivateUserController {

    private final AuthorizationService authz;
    private final UserManagementService userSvc;

    /** Production constructor — uses framework registries. */
    public DeactivateUserController() {
        this(AuthzRegistry.authorizationService(), AuthzRegistry.userService());
    }

    /** Testing constructor — allows injecting mocks. */
    DeactivateUserController(final AuthorizationService authz, final UserManagementService userSvc) {
        this.authz = authz;
        this.userSvc = userSvc;
    }

    /** Returns all currently active users for selection (disable flow). */
    public Iterable<SystemUser> activeUsers() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return userSvc.activeUsers();
    }

    /** Returns all currently inactive users for selection (enable flow). */
    public Iterable<SystemUser> deactivatedUsers() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return userSvc.deactivatedUsers();
    }

    /**
     * Disable (deactivate) a user account — AC 032.2 / AC 032.5.
     * An administrator cannot deactivate their own account.
     *
     * @param user the active user to disable
     * @return the updated SystemUser
     * @throws IllegalArgumentException if the admin tries to disable their own account
     */
    public SystemUser deactivateUser(final SystemUser user) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final String currentUsername = authz.session()
                .map(s -> s.authenticatedUser().identity().toString())
                .orElse("");
        if (currentUsername.equalsIgnoreCase(user.identity().toString())) {
            throw new IllegalArgumentException("An administrator cannot disable their own account.");
        }

        return userSvc.deactivateUser(user);
    }

    /**
     * Enable (reactivate) a previously disabled user account — AC 032.3.
     *
     * @param user the inactive user to re-enable
     * @return the updated SystemUser
     */
    public SystemUser activateUser(final SystemUser user) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return userSvc.activateUser(user);
    }
}
