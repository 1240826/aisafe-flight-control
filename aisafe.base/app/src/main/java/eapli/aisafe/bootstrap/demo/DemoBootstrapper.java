/*
 * Copyright (c) 2013-2024 the original author or authors.
 *
 * MIT License
 */
package eapli.aisafe.bootstrap.demo;

import eapli.aisafe.bootstrap.AISafeBootstrapper;
import eapli.aisafe.bootstrap.AISafeDemoDataBootstrapper;
import eapli.framework.actions.Action;
import eapli.framework.infrastructure.authz.application.AuthenticationService;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.strings.util.Strings;
import eapli.framework.validations.Invariants;

/**
 * Bootstrapping demo data.
 */
@SuppressWarnings("squid:S106")
public class DemoBootstrapper implements Action {

	private static final String POWERUSER_A1 = "poweruserA1";
	private static final String POWERUSER = "poweruser";

	private final AuthorizationService authz = AuthzRegistry.authorizationService();
	private final AuthenticationService authenticationService = AuthzRegistry.authenticationService();

	@Override
	public boolean execute() {
		final Action[] actions = { new AISafeDemoDataBootstrapper(), };

		authenticateForBootstrapping();

		var ret = true;
		for (final Action boot : actions) {
			System.out.println("Bootstrapping " + nameOfEntity(boot) + "...");
			ret &= boot.execute();
		}
		return ret;
	}

	protected void authenticateForBootstrapping() {
		authenticationService.authenticate(POWERUSER, POWERUSER_A1);
		Invariants.ensure(authz.hasSession());
	}

	private String nameOfEntity(final Action boot) {
		final var name = boot.getClass().getSimpleName();
		return Strings.left(name, name.length() - "Bootstrapper".length());
	}
}
