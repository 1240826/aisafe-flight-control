package eapli.exemplo.infrastructure.bootstrapers;

import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.actions.Action;
import eapli.framework.infrastructure.authz.domain.model.Role;

import java.util.HashSet;
import java.util.Set;

/**
 * Bootstraps master AISafe users:
 * - admin1 / Password1 — ADMIN role
 * - backoffice1 / Password1 — BACKOFFICE_OPERATOR role
 * US030, US031.
 */
public class AISafeMasterUsersBootstrapper extends AbstractUserBootstrapper implements Action {

    @Override
    public boolean execute() {
        registerAISafeAdmin("admin1", TestDataConstants.PASSWORD1, "Admin", "AISafe",
                "admin@aisafe.local");
        registerBackofficeOperator("backoffice1", TestDataConstants.PASSWORD1, "Backoffice", "Operator",
                "backoffice@aisafe.local");
        return true;
    }

    private void registerAISafeAdmin(final String username, final String password,
                                      final String firstName, final String lastName, final String email) {
        final Set<Role> roles = new HashSet<>();
        roles.add(AISafeRoles.ADMIN);
        registerUser(username, password, firstName, lastName, email, roles);
    }

    private void registerBackofficeOperator(final String username, final String password,
                                             final String firstName, final String lastName,
                                             final String email) {
        final Set<Role> roles = new HashSet<>();
        roles.add(AISafeRoles.BACKOFFICE_OPERATOR);
        registerUser(username, password, firstName, lastName, email, roles);
    }
}
