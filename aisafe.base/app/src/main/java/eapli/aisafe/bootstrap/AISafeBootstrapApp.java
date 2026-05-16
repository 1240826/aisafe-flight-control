/*
 * Copyright (c) 2013-2024 the original author or authors.
 *
 * MIT License
 */
package eapli.aisafe.bootstrap;

import eapli.aisafe.ui.BaseApp;
import eapli.aisafe.bootstrap.demo.DemoBootstrapper;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AISafePasswordPolicy;
import eapli.framework.collections.util.ArrayPredicates;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.pubsub.EventDispatcher;

/**
 * Bootstrapping data app
 */
@SuppressWarnings("squid:S106")
public final class AISafeBootstrapApp extends BaseApp {
	/**
	 * avoid instantiation of this class.
	 */
	private AISafeBootstrapApp() {
	}

	private boolean isToBootstrapDemoData;
	private boolean isToRunSampleE2E;

	public static void main(final String[] args) {

		AuthzRegistry.configure(PersistenceContext.repositories().users(), new AISafePasswordPolicy(),
				new PlainTextEncoder());

		new AISafeBootstrapApp().run(args);
	}

	@Override
	protected void doMain(final String[] args) {
		handleArgs(args);

		System.out.println("\n\n------- MASTER DATA -------");
		new AISafeBootstrapper().execute();

		if (isToBootstrapDemoData) {
			System.out.println("\n\n------- DEMO DATA -------");
			new DemoBootstrapper().execute();
		}
		if (isToRunSampleE2E) {
			System.out.println("\n\n------- BASIC SCENARIO -------");
			new DemoSmokeTester().execute();
		}
	}

	private void handleArgs(final String[] args) {
		isToRunSampleE2E = ArrayPredicates.contains(args, "-smoke:basic");
		if (isToRunSampleE2E) {
			isToBootstrapDemoData = true;
		} else {
			isToBootstrapDemoData = ArrayPredicates.contains(args, "-bootstrap:demo");
		}
	}

	@Override
	protected String appTitle() {
		return "AISafe Bootstrapping data ";
	}

	@Override
	protected String appGoodbye() {
		return "AISafe Bootstrap data done.";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doSetupEventHandlers(final EventDispatcher dispatcher) {
		// no event handlers needed for bootstrap
	}
}
