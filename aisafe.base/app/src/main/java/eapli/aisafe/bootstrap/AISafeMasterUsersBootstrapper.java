package eapli.aisafe.bootstrap;

import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.actions.Action;
import eapli.framework.infrastructure.authz.domain.model.Role;

import java.util.Set;

/**
 * Bootstraps master AISafe users.
 *
 * <pre>
 * USERNAME      PASSWORD   ROLES                                           PURPOSE
 * -----------   ---------  ----------------------------------------------  -----------------------
 * demo          Password1  ALL roles                                       MVP full-demo superuser
 * admin1        Password1  ADMIN                                           User management / settings
 * backoffice1   Password1  BACKOFFICE_OPERATOR                             Backoffice configuration
 * atc1          Password1  ATC_COLLABORATOR                                Aircraft / fleet management
 * fco1          Password1  FLIGHT_CONTROL_OPERATOR                         Flight simulation / monitoring
 * weather1      Password1  WEATHER_PERSON                                  Weather data registration
 * </pre>
 *
 * US030, US031.
 */
public class AISafeMasterUsersBootstrapper extends AbstractUserBootstrapper implements Action {

    @Override
    public boolean execute() {
        // ── Superuser: full access to every menu (MVP demo) ──────────────────
        registerUser("demo", TestDataConstants.PASSWORD1, "Demo", "Superuser",
                "demo@aisafe.local",
                Set.of(AISafeRoles.ADMIN,
                       AISafeRoles.BACKOFFICE_OPERATOR,
                       AISafeRoles.ATC_COLLABORATOR,
                       AISafeRoles.FLIGHT_CONTROL_OPERATOR,
                       AISafeRoles.WEATHER_PERSON,
                       AISafeRoles.PILOT));

        // ── Role-specific users ───────────────────────────────────────────────
        registerUser("admin1", TestDataConstants.PASSWORD1, "Admin", "AISafe",
                "admin@aisafe.local",
                Set.of(AISafeRoles.ADMIN));

        registerUser("backoffice1", TestDataConstants.PASSWORD1, "Backoffice", "Operator",
                "backoffice@aisafe.local",
                Set.of(AISafeRoles.BACKOFFICE_OPERATOR));

        registerUser("atc1", TestDataConstants.PASSWORD1, "ATC", "Collaborator",
                "atc@aisafe.local",
                Set.of(AISafeRoles.ATC_COLLABORATOR));

        registerUser("fco1", TestDataConstants.PASSWORD1, "Flight", "Controller",
                "fco@aisafe.local",
                Set.of(AISafeRoles.FLIGHT_CONTROL_OPERATOR));

        registerUser("weather1", TestDataConstants.PASSWORD1, "Weather", "Person",
                "weather@aisafe.local",
                Set.of(AISafeRoles.WEATHER_PERSON));

        return true;
    }
}
