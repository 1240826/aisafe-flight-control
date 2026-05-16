package eapli.aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.Role;

/**
 * Application roles for AISafe.
 * US030 — auth infrastructure.
 */
public final class AISafeRoles {

    public static final Role ADMIN = Role.valueOf("ADMIN");
    public static final Role BACKOFFICE_OPERATOR = Role.valueOf("BACKOFFICE_OPERATOR");
    public static final Role ATC_COLLABORATOR = Role.valueOf("ATC_COLLABORATOR");
    public static final Role FLIGHT_CONTROL_OPERATOR = Role.valueOf("FLIGHT_CONTROL_OPERATOR");
    public static final Role WEATHER_PERSON = Role.valueOf("WEATHER_PERSON");
    /** Internal bootstrap role — used by AISafeBootstrapper to circumvent auth. */
    public static final Role POWER_USER = Role.valueOf("POWER_USER");

    private AISafeRoles() {
        // utility
    }

    public static Role[] nonUserValues() {
        return new Role[]{ADMIN, BACKOFFICE_OPERATOR, ATC_COLLABORATOR, FLIGHT_CONTROL_OPERATOR, WEATHER_PERSON};
    }
}
