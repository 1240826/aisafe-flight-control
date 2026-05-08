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
package eapli.exemplo.usermanagement.application;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Set;

import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.usermanagement.domain.UserSecurityProfile;
import eapli.aisafe.usermanagement.repositories.UserSecurityProfileRepository;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.exemplo.usermanagement.domain.ExemploRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.time.util.CurrentTimeCalendars;

/**
 * US031 — Register Users.
 * Registers a new SystemUser and saves a companion UserSecurityProfile
 * with the security clearance expiry date (AC 031.7).
 */
@UseCaseController
public class AddUserController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserManagementService userSvc = AuthzRegistry.userService();
    private final UserSecurityProfileRepository profileRepo =
            PersistenceContext.repositories().userSecurityProfiles();

    /**
     * Available AISafe roles for assignment.
     */
    public Role[] getRoleTypes() {
        return AISafeRoles.nonUserValues();
    }

    /**
     * Register a new user with security clearance expiry date (AC 031.7).
     *
     * @param username                   unique login name
     * @param password                   must comply with AISafePasswordPolicy
     * @param firstName                  first name
     * @param lastName                   last name
     * @param email                      email address
     * @param roles                      at least one AISafe role
     * @param securityClearanceExpiryDate must be today or in the future
     * @param createdOn                  creation timestamp
     * @return the registered SystemUser
     */
    public SystemUser addUser(final String username, final String password,
            final String firstName, final String lastName,
            final String email, final Set<Role> roles,
            final LocalDate securityClearanceExpiryDate,
            final Calendar createdOn) {
        // POWER_USER is the internal bootstrap role used by ExemploBootstrapper
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, ExemploRoles.POWER_USER);

        final SystemUser user = userSvc.registerNewUser(
                username, password, firstName, lastName, email, roles, createdOn);

        // AC 031.7 — persist security clearance expiry date
        profileRepo.save(new UserSecurityProfile(username, securityClearanceExpiryDate));

        return user;
    }

    /**
     * Convenience overload using current timestamp and required clearance date.
     */
    public SystemUser addUser(final String username, final String password,
            final String firstName, final String lastName,
            final String email, final Set<Role> roles,
            final LocalDate securityClearanceExpiryDate) {
        return addUser(username, password, firstName, lastName, email, roles,
                securityClearanceExpiryDate, CurrentTimeCalendars.now());
    }
}
