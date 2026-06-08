/*
 * Copyright (c) 2013-2024 the original author or authors.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package eapli.aisafe.ui;

import eapli.aisafe.server.PilotServerDaemon;
import eapli.aisafe.server.WeatherServerDaemon;
import eapli.aisafe.ui.authz.LoginUI;
import eapli.aisafe.infrastructure.Application;
import eapli.aisafe.infrastructure.authz.AuthenticationCredentialHandler;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AISafePasswordPolicy;
import eapli.aisafe.usermanagement.domain.UserSecurityProfile;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.pubsub.EventDispatcher;
import eapli.aisafe.server.AtcServerDaemon;

import java.util.Optional;

/**
 *
 * @author Paulo Gandra Sousa
 */
@SuppressWarnings("squid:S106")
public final class AISafeBackoffice extends BaseApp {

	/**
	 * avoid instantiation of this class.
	 */
	private AISafeBackoffice() {
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(final String[] args) {

		AuthzRegistry.configure(PersistenceContext.repositories().users(), new AISafePasswordPolicy(),
				new PlainTextEncoder());

		// ── RCOMP: start embedded TCP servers (US044, US078, US086) ──────────────
		// The TCP server and UDP client are embedded in the Main Application
		// as required by the deployment diagram (Figure 1, RCOMP Project 2).
		startRemoteServices();

		new AISafeBackoffice().run(args);
	}

	/**
	 * Starts the three TCP server daemons as background threads.
	 * Each daemon handles one remote service (US044/US078/US086) and sends
	 * UDP access events to the Remote Accesses Logging Server (US090).
	 */
	private static void startRemoteServices() {
		// US044 — Weather Person remote access (port 1044)
		final Thread t1 = new Thread(new WeatherServerDaemon(), "daemon-weather-US44");
		t1.setDaemon(true);
		t1.start();

		// US086 — Pilot (FCO) remote access (port 1086)
		final Thread t2 = new Thread(new PilotServerDaemon(), "daemon-pilot-US86");
		t2.setDaemon(true);
		t2.start();

		// US078 — ATCC remote access (port 1078)
		final Thread t3 = new Thread(new AtcServerDaemon(), "daemon-atcc-US78");
		t3.setDaemon(true);
		t3.start();

		System.out.println("[RCOMP] Remote services started (ports 1044, 1078, 1086).");
	}

	@Override
	protected void doMain(final String[] args) {
		// login and go to main menu
		if (new LoginUI(new AuthenticationCredentialHandler()).show()) {
			// Security clearance check (AC for US030/US031).
			// Must be done here — outside the auth handler so we can use an explicit
			// JPA transaction (JpaAutoTxRepository reads require an active transaction).
			if (!hasClearance()) {
				System.out.printf("%n  [!] Access denied: your security clearance has expired."
						+ " Contact an administrator.%n%n");
				return;
			}
			// go to main menu
			final var menu = new MainMenu();
			menu.mainLoop();
		}
	}

	/**
	 * Checks whether the currently authenticated user has a valid security clearance.
	 * Returns {@code true} if clearance is valid or no profile exists (no constraint).
	 * Uses an explicit JPA transaction to ensure the repository read succeeds even
	 * when the authentication service has already committed and closed its own context.
	 */
	private boolean hasClearance() {
		final var session = AuthzRegistry.authorizationService().session();
		if (session.isEmpty()) {
			return true; // nobody logged in — nothing to check
		}
		final String username = session.get().authenticatedUser().identity().toString();

		final TransactionalContext tx = PersistenceContext.repositories().newTransactionalContext();
		tx.beginTransaction();
		try {
			final Optional<UserSecurityProfile> profile =
					PersistenceContext.repositories().userSecurityProfiles(tx).findByUsername(username);
			tx.commit();
			// No profile stored = no constraint on this user
			return profile.map(UserSecurityProfile::isClearanceValid).orElse(true);
		} catch (final Exception e) {
			try { tx.rollback(); } catch (final Exception ignored) { /* best effort */ }
			// If the check itself fails, fail-open so legitimate users are not locked out
			return true;
		} finally {
			try { tx.close(); } catch (final Exception ignored) { /* best effort */ }
		}
	}

	@Override
	protected String appTitle() {
		return "AISafe Back Office - Air Safety Flight Control System";
	}

	@Override
	protected String appGoodbye() {
		return "Thank you for using AISafe. Stay safe in the skies!";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doSetupEventHandlers(final EventDispatcher dispatcher) {
		// no event handlers needed for basic backoffice
	}
}
