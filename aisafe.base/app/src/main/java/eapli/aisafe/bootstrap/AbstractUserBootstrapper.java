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
package eapli.aisafe.bootstrap;

import java.time.LocalDate;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.usermanagement.application.AddUserController;
import eapli.aisafe.usermanagement.application.ListUsersController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.Username;

public class AbstractUserBootstrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUserBootstrapper.class);

    /** Bootstrap users get a clearance valid for 10 years. */
    private static final LocalDate BOOTSTRAP_CLEARANCE_EXPIRY = LocalDate.now().plusYears(10);

    final AddUserController userController = new AddUserController();
    final ListUsersController listUserController = new ListUsersController();

    public AbstractUserBootstrapper() {
        super();
    }

    /**
     * Register a bootstrap user with a default 10-year security clearance expiry.
     * Idempotent: returns existing user if the username already exists, avoiding
     * Hibernate 6 RollbackException on duplicate-key violations at commit time.
     */
    protected SystemUser registerUser(final String username, final String password,
            final String firstName, final String lastName,
            final String email, final Set<Role> roles) {
        // Pre-check: return existing user instead of triggering a constraint violation
        final var existing = listUserController.find(Username.valueOf(username));
        if (existing.isPresent()) {
            LOGGER.warn("User '{}' already exists — skipping creation.", username);
            return existing.get();
        }
        SystemUser u = null;
        try {
            u = userController.addUser(username, password, firstName, lastName, email, roles,
                    BOOTSTRAP_CLEARANCE_EXPIRY);
            LOGGER.debug("Bootstrapped user: {}", username);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            u = listUserController.find(Username.valueOf(username)).orElseThrow(() -> e);
        }
        return u;
    }
}
