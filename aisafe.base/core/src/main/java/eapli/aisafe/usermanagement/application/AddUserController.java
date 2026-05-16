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

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Set;

import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.usermanagement.domain.UserSecurityProfile;
import eapli.aisafe.usermanagement.repositories.UserSecurityProfileRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
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

    private final AuthorizationService authz;
    private final UserManagementService userSvc;
    private final UserSecurityProfileRepository profileRepo;

    /** Production constructor — uses framework registries. */
    public AddUserController() {
        this(AuthzRegistry.authorizationService(),
                AuthzRegistry.userService(),
                PersistenceContext.repositories().userSecurityProfiles());
    }

    /** Testing constructor — allows injecting mocks. */
    AddUserController(final AuthorizationService authz,
                      final UserManagementService userSvc,
                      final UserSecurityProfileRepository profileRepo) {
        this.authz = authz;
        this.userSvc = userSvc;
        this.profileRepo = profileRepo;
    }

    /**
     * Available AISafe roles for assignment.
     */
    public Role[] getRoleTypes() {
        return AISafeRoles.nonUserValues();
    }

    /**
     * Register a new user with security clearance expiry date and phone number.
     * Phone is required per spec §3.1.1: "A user also has a name and phone number."
     *
     * @param username                   unique login name
     * @param password                   must comply with AISafePasswordPolicy
     * @param firstName                  first name
     * @param lastName                   last name
     * @param email                      email address
     * @param roles                      at least one AISafe role
     * @param securityClearanceExpiryDate must be today or in the future
     * @param phone                      contact phone number (non-blank)
     * @param createdOn                  creation timestamp
     * @return the registered SystemUser
     */
    public SystemUser addUser(final String username, final String password,
            final String firstName, final String lastName,
            final String email, final Set<Role> roles,
            final LocalDate securityClearanceExpiryDate,
            final String phone,
            final Calendar createdOn) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR, AISafeRoles.POWER_USER);

        final SystemUser user = userSvc.registerNewUser(
                username, password, firstName, lastName, email, roles, createdOn);

        // AC 031.7 — persist security clearance expiry date and phone
        profileRepo.save(new UserSecurityProfile(username, securityClearanceExpiryDate, phone));

        return user;
    }

    /**
     * Convenience overload using current timestamp.
     */
    public SystemUser addUser(final String username, final String password,
            final String firstName, final String lastName,
            final String email, final Set<Role> roles,
            final LocalDate securityClearanceExpiryDate,
            final String phone) {
        return addUser(username, password, firstName, lastName, email, roles,
                securityClearanceExpiryDate, phone, CurrentTimeCalendars.now());
    }

    /**
     * Backward-compatible overload without phone — used by bootstrap only.
     * Phone will be stored as {@code null}.
     */
    public SystemUser addUser(final String username, final String password,
            final String firstName, final String lastName,
            final String email, final Set<Role> roles,
            final LocalDate securityClearanceExpiryDate) {
        return addUser(username, password, firstName, lastName, email, roles,
                securityClearanceExpiryDate, CurrentTimeCalendars.now());
    }

    /**
     * Internal overload used by the backward-compat method above.
     */
    private SystemUser addUser(final String username, final String password,
            final String firstName, final String lastName,
            final String email, final Set<Role> roles,
            final LocalDate securityClearanceExpiryDate,
            final Calendar createdOn) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR, AISafeRoles.POWER_USER);

        final SystemUser user = userSvc.registerNewUser(
                username, password, firstName, lastName, email, roles, createdOn);

        // Bootstrap path — no phone provided
        profileRepo.save(new UserSecurityProfile(username, securityClearanceExpiryDate));

        return user;
    }
}
