package eapli.aisafe.server;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.remote.RemoteProtocol;
import eapli.aisafe.remote.UdpAccessLogger;
import eapli.aisafe.usermanagement.domain.UserSecurityProfile;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.application.Authenticator;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.Role;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Base TCP client handler.
 *
 * <p>Handles the auth handshake, UDP event logging (US090), and the main read-dispatch loop.
 * Subclasses implement {@link #handleCommand(String, String[])} to process commands once
 * the client is authenticated.
 */
public abstract class AbstractClientHandler implements Runnable {

    private static final String LOG_HOST_PROPERTY = "aisafe.logging.host";
    private static final String DEFAULT_LOG_HOST   = "localhost";

    private final Socket clientSocket;
    private final String serviceId;
    private final Role requiredRole;
    private final UdpAccessLogger logger;

    private String authenticatedUsername = "anonymous";

    protected AbstractClientHandler(final Socket clientSocket,
                                     final String serviceId,
                                     final Role requiredRole) {
        this.clientSocket = clientSocket;
        this.serviceId    = serviceId;
        this.requiredRole = requiredRole;
        final String logHost = System.getProperty(LOG_HOST_PROPERTY, DEFAULT_LOG_HOST);
        this.logger = new UdpAccessLogger(logHost);
    }

    @Override
    public final void run() {
        final String clientIp   = clientSocket.getInetAddress().getHostAddress();
        final int    clientPort = clientSocket.getPort();

        try (clientSocket;
             final BufferedReader in  = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
             final PrintWriter    out = new PrintWriter(
                     new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), false)) {

            boolean loggedOut = false;
            String line;

            while ((line = in.readLine()) != null) {
                final String[] fields = RemoteProtocol.parse(line);
                if (fields.length == 0) continue;

                final String cmd = fields[0].toUpperCase();

                // ── QUIT ────────────────────────────────────────────────────
                if (RemoteProtocol.CMD_QUIT.equals(cmd)) {
                    out.print(RemoteProtocol.RESP_BYE + "\n");
                    out.flush();
                    if (isAuthenticated()) {
                        logger.logout(authenticatedUsername, clientIp, clientPort, serviceId);
                    }
                    loggedOut = true;
                    break;
                }

                // ── AUTH ────────────────────────────────────────────────────
                if (RemoteProtocol.CMD_AUTH.equals(cmd)) {
                    if (fields.length < 3) {
                        out.print(RemoteProtocol.RESP_AUTH_FAIL + RemoteProtocol.SEP
                                + "Usage: AUTH|username|password\n");
                        out.flush();
                        continue;
                    }
                    final String username = fields[1];
                    final String password = fields[2];

                    final Authenticator auth = AuthzRegistry.authenticationService();

                    // Step 1 — full authentication (credentials + required role)
                    final java.util.Optional<UserSession> session =
                            auth.authenticate(username, password, requiredRole);

                    if (session.isEmpty()) {
                        // Step 2 — differentiate: wrong credentials vs. wrong role
                        final var credOnly = auth.authenticate(username, password);
                        if (credOnly.isPresent()) {
                            authFail(out, username, "INSUFFICIENT_ROLE");
                        } else {
                            authFail(out, username, "Invalid credentials");
                        }
                        continue;
                    }

                    // Step 3 — check security clearance expiry (US030.4)
                    if (!isClearanceValid(username)) {
                        authFail(out, username, "SECURITY_CLEARANCE_EXPIRED");
                        continue;
                    }

                    // All checks passed
                    authenticatedUsername = username;
                    logger.loginOk(username, clientIp, clientPort, serviceId);
                    out.print(RemoteProtocol.RESP_AUTH_OK + "\n");
                    out.flush();
                    continue;
                }

                // ── Require auth for all other commands ─────────────────────
                if (!isAuthenticated()) {
                    out.print(RemoteProtocol.err("NOT_AUTHENTICATED"));
                    out.flush();
                    continue;
                }

                // ── Dispatch to subclass ────────────────────────────────────
                final String response = handleCommand(cmd, fields);
                out.print(response);
                out.flush();
            }

            // Client closed connection without QUIT
            if (!loggedOut && isAuthenticated()) {
                logger.disconnect(authenticatedUsername, clientIp, clientPort, serviceId);
            }

        } catch (final IOException e) {
            if (isAuthenticated()) {
                logger.disconnect(authenticatedUsername, clientIp, clientPort, serviceId);
            }
            System.err.printf("[%s] Connection error from %s:%d — %s%n",
                    serviceId, clientIp, clientPort, e.getMessage());
        }
    }

    private void authFail(final PrintWriter out, final String username, final String reason) {
        logger.loginFail(username,
                clientSocket.getInetAddress().getHostAddress(),
                clientSocket.getPort(), serviceId);
        out.print(RemoteProtocol.RESP_AUTH_FAIL + RemoteProtocol.SEP + reason + "\n");
        out.flush();
    }

    private boolean isClearanceValid(final String username) {
        final TransactionalContext tx = PersistenceContext.repositories().newTransactionalContext();
        tx.beginTransaction();
        try {
            final java.util.Optional<UserSecurityProfile> profile =
                    PersistenceContext.repositories().userSecurityProfiles(tx).findByUsername(username);
            tx.commit();
            return profile.map(UserSecurityProfile::isClearanceValid).orElse(true);
        } catch (final Exception e) {
            try { tx.rollback(); } catch (final Exception ignored) { }
            return true; // fail-open: if check fails, allow login
        } finally {
            try { tx.close(); } catch (final Exception ignored) { }
        }
    }

    private boolean isAuthenticated() {
        return !"anonymous".equals(authenticatedUsername);
    }

    /**
     * Dispatch a command that has already passed authentication.
     *
     * @param cmd    the command token (already upper-cased)
     * @param fields all pipe-split fields including the command at index 0
     * @return a full response line to send back (must end with {@code \n})
     */
    protected abstract String handleCommand(String cmd, String[] fields);
}
