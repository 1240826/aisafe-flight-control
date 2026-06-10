package eapli.aisafe.ui.jfx.controller;

import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.util.Optional;

public final class SessionManager {

    private static UserSession currentSession;

    private SessionManager() {
    }

    public static void login(final UserSession session) {
        currentSession = session;
    }

    public static void logout() {
        currentSession = null;
    }

    public static Optional<UserSession> session() {
        return Optional.ofNullable(currentSession);
    }

    public static boolean isAuthenticated() {
        return currentSession != null;
    }

    public static boolean hasRole(final Role role) {
        return session()
                .map(s -> s.authenticatedUser())
                .map(u -> u.hasAny(role))
                .orElse(false);
    }

    public static boolean hasAnyRole(final Role... roles) {
        return session()
                .map(s -> s.authenticatedUser())
                .map(u -> {
                    for (final Role r : roles) {
                        if (u.hasAny(r)) return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    public static String currentUsername() {
        return session()
                .map(s -> s.authenticatedUser())
                .map(u -> u.username().toString())
                .orElse("Unknown");
    }

    public static String currentDisplayName() {
        return session()
                .map(s -> s.authenticatedUser())
                .map(u -> u.name().toString())
                .orElse("Unknown");
    }
}
