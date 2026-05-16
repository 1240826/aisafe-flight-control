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
package eapli.aisafe.ui;

import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.ui.aircontrolarea.ListAirControlAreasUI;
import eapli.aisafe.ui.aircontrolarea.RegisterAirControlAreaUI;
import eapli.aisafe.ui.aircraft.AddAircraftUI;
import eapli.aisafe.ui.aircraft.DecommissionAircraftUI;
import eapli.aisafe.ui.aircraft.ListCompanyFleetUI;
import eapli.aisafe.ui.aircraftmodel.AddEngineVariantUI;
import eapli.aisafe.ui.aircraftmodel.CreateAircraftModelUI;
import eapli.aisafe.ui.aircraftmodel.RemoveEngineVariantUI;
import eapli.aisafe.ui.airport.CreateAirportUI;
import eapli.aisafe.ui.airport.ListAirportsUI;
import eapli.aisafe.ui.collaborator.AddCollaboratorUI;
import eapli.aisafe.ui.collaborator.DisableCollaboratorUI;
import eapli.aisafe.ui.collaborator.EditCollaboratorUI;
import eapli.aisafe.ui.collaborator.ListCollaboratorsUI;
import eapli.aisafe.ui.company.RegisterAirTransportCompanyUI;
import eapli.aisafe.ui.enginemodel.CreateEngineModelUI;
import eapli.aisafe.ui.manufacturer.RegisterManufacturerUI;
import eapli.aisafe.ui.weatherdata.RegisterWeatherDataUI;
import eapli.aisafe.infrastructure.Application;
import eapli.aisafe.ui.authz.ActivateUserUI;
import eapli.aisafe.ui.authz.AddUserUI;
import eapli.aisafe.ui.authz.DeactivateUserAction;
import eapli.aisafe.ui.authz.ListUsersAction;
import eapli.aisafe.ui.authz.MyUserMenu;
import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.actions.menu.MenuItem;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.ExitWithMessageAction;
import eapli.framework.presentation.console.ShowMessageAction;
import eapli.framework.presentation.console.menu.HorizontalMenuRenderer;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

/**
 * Main backoffice menu.
 * US031 — Add User, US032 — Disable/Enable User, US033 — List Users.
 * AISafe domain menus: Manufacturers, Engine Models, Aircraft Models,
 * Air Control Areas, Airports, Companies, Collaborators, Aircraft, Weather Data.
 */
public class MainMenu extends AbstractUI {

	private static final String RETURN_LABEL = "Return ";
	private static final int EXIT_OPTION = 0;

	// USERS submenu
	private static final int ADD_USER_OPTION = 1;
	private static final int LIST_USERS_OPTION = 2;
	private static final int DEACTIVATE_USER_OPTION = 3;
	private static final int ACTIVATE_USER_OPTION = 4;

	// SETTINGS submenu
	private static final int SYSTEM_INFO_OPTION = 1;

	// MANUFACTURERS submenu
	private static final int REGISTER_MANUFACTURER_OPTION = 1;

	// ENGINE MODELS submenu
	private static final int CREATE_ENGINE_MODEL_OPTION = 1;

	// AIRCRAFT MODELS submenu
	private static final int CREATE_AIRCRAFT_MODEL_OPTION = 1;
	private static final int ADD_ENGINE_VARIANT_OPTION = 2;
	private static final int REMOVE_ENGINE_VARIANT_OPTION = 3;

	// AIR CONTROL AREAS submenu
	private static final int REGISTER_ACA_OPTION = 1;
	private static final int LIST_ACA_OPTION = 2;

	// AIRPORTS submenu
	private static final int CREATE_AIRPORT_OPTION = 1;
	private static final int LIST_AIRPORTS_OPTION = 2;

	// COMPANIES submenu
	private static final int REGISTER_COMPANY_OPTION = 1;

	// COLLABORATORS submenu
	private static final int ADD_COLLABORATOR_OPTION = 1;
	private static final int LIST_COLLABORATORS_OPTION = 2;
	private static final int EDIT_COLLABORATOR_OPTION = 3;
	private static final int DISABLE_COLLABORATOR_OPTION = 4;

	// AIRCRAFT submenu
	private static final int ADD_AIRCRAFT_OPTION = 1;
	private static final int LIST_FLEET_OPTION = 2;
	private static final int DECOMMISSION_AIRCRAFT_OPTION = 3;

	// WEATHER DATA submenu
	private static final int REGISTER_WEATHER_DATA_OPTION = 1;

	// MAIN MENU slots
	private static final int MY_USER_OPTION = 1;
	private static final int USERS_OPTION = 2;
	private static final int MANUFACTURERS_OPTION = 3;
	private static final int ENGINE_MODELS_OPTION = 4;
	private static final int AIRCRAFT_MODELS_OPTION = 5;
	private static final int ACA_OPTION = 6;
	private static final int AIRPORTS_OPTION = 7;
	private static final int COMPANIES_OPTION = 8;
	private static final int COLLABORATORS_OPTION = 9;
	private static final int AIRCRAFT_OPTION = 10;
	private static final int WEATHER_DATA_OPTION = 11;
	private static final int SETTINGS_OPTION = 12;

	private static final String SEPARATOR_LABEL = "--------------";

	private final AuthorizationService authz = AuthzRegistry.authorizationService();

	@Override
	public boolean show() {
		drawFormTitle();
		return doShow();
	}

	@Override
	public boolean doShow() {
		final var menu = buildMainMenu();
		final MenuRenderer renderer;
		if (Application.settings().isMenuLayoutHorizontal()) {
			renderer = new HorizontalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
		} else {
			renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
		}
		return renderer.render();
	}

	@Override
	public String headline() {
		return authz.session().map(s -> "Backoffice [ @" + s.authenticatedUser().identity() + " ]")
				.orElse("Backoffice [ ==Anonymous== ]");
	}

	/**
	 * Runs a submenu in a loop until the user chooses "Return" (Actions.SUCCESS → true).
	 * Normal actions return {@code false}, so the loop keeps going.
	 * Returns {@code false} so the main-menu renderer knows the session is still active.
	 */
	private boolean runSubMenu(final Menu submenu) {
		final var renderer = new VerticalMenuRenderer(submenu, MenuItemRenderer.DEFAULT);
		boolean exit = false;
		while (!exit) {
			exit = renderer.render();
		}
		return false;
	}

	/** Returns true if the authenticated user has at least one of the given roles. */
	private boolean hasAnyRole(final eapli.framework.infrastructure.authz.domain.model.Role... roles) {
		for (final var role : roles) {
			if (authz.isAuthenticatedUserAuthorizedTo(role)) return true;
		}
		return false;
	}

	private Menu buildMainMenu() {
		final var mainMenu = new Menu();
		final Menu myUserMenu = new MyUserMenu();
		mainMenu.addItem(MY_USER_OPTION, "My account >", () -> runSubMenu(myUserMenu));

		if (!Application.settings().isMenuLayoutHorizontal()) {
			mainMenu.addItem(MenuItem.separator(SEPARATOR_LABEL));
		}

		// UC07 — Manage users: Admin + Backoffice Operator
		if (hasAnyRole(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR)) {
			mainMenu.addItem(USERS_OPTION, "Users >", () -> runSubMenu(buildUsersMenu()));
		}

		// UC01-UC06 — Backoffice configuration: Backoffice Operator only
		if (hasAnyRole(AISafeRoles.BACKOFFICE_OPERATOR)) {
			// UC03 — Manufacturers
			mainMenu.addItem(MANUFACTURERS_OPTION, "Manufacturers >", () -> runSubMenu(buildManufacturersMenu()));
			// UC05 — Engine Models
			mainMenu.addItem(ENGINE_MODELS_OPTION, "Engine Models >", () -> runSubMenu(buildEngineModelsMenu()));
			// UC04 — Aircraft Models
			mainMenu.addItem(AIRCRAFT_MODELS_OPTION, "Aircraft Models >", () -> runSubMenu(buildAircraftModelsMenu()));
			// UC01 — Air Control Areas
			mainMenu.addItem(ACA_OPTION, "Air Control Areas >", () -> runSubMenu(buildAirControlAreasMenu()));
			// UC02 — Airports
			mainMenu.addItem(AIRPORTS_OPTION, "Airports >", () -> runSubMenu(buildAirportsMenu()));
			// UC06 — Companies
			mainMenu.addItem(COMPANIES_OPTION, "Air Transport Companies >", () -> runSubMenu(buildCompaniesMenu()));
			// UC23 — Collaborators / pilot management
			mainMenu.addItem(COLLABORATORS_OPTION, "Collaborators >", () -> runSubMenu(buildCollaboratorsMenu()));
		}

		// UC21 — Aircraft management: Air Transport Company Collaborator
		if (hasAnyRole(AISafeRoles.ATC_COLLABORATOR)) {
			mainMenu.addItem(AIRCRAFT_OPTION, "Aircraft >", () -> runSubMenu(buildAircraftMenu()));
		}

		// UC30 — Weather data: Weather Person only
		if (hasAnyRole(AISafeRoles.WEATHER_PERSON)) {
			mainMenu.addItem(WEATHER_DATA_OPTION, "Weather Data >", () -> runSubMenu(buildWeatherDataMenu()));
		}

		// Settings: Admin only
		if (hasAnyRole(AISafeRoles.ADMIN)) {
			mainMenu.addItem(SETTINGS_OPTION, "Settings >", () -> runSubMenu(buildAdminSettingsMenu()));
		}

		if (!Application.settings().isMenuLayoutHorizontal()) {
			mainMenu.addItem(MenuItem.separator(SEPARATOR_LABEL));
		}

		mainMenu.addItem(EXIT_OPTION, "Exit", new ExitWithMessageAction("Bye, Bye"));
		return mainMenu;
	}

	private Menu buildUsersMenu() {
		final var menu = new Menu("Users >");
		menu.addItem(ADD_USER_OPTION, "Add User", () -> { new AddUserUI().show(); return false; });
		menu.addItem(LIST_USERS_OPTION, "List all Users", () -> { new ListUsersAction().execute(); return false; });
		menu.addItem(DEACTIVATE_USER_OPTION, "Disable User", () -> { new DeactivateUserAction().execute(); return false; });
		menu.addItem(ACTIVATE_USER_OPTION, "Enable User", () -> { new ActivateUserUI().show(); return false; });
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildManufacturersMenu() {
		final var menu = new Menu("Manufacturers >");
		menu.addItem(REGISTER_MANUFACTURER_OPTION, "Register Manufacturer", () -> { new RegisterManufacturerUI().show(); return false; });
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildEngineModelsMenu() {
		final var menu = new Menu("Engine Models >");
		menu.addItem(CREATE_ENGINE_MODEL_OPTION, "Create Engine Model", () -> { new CreateEngineModelUI().show(); return false; });
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildAircraftModelsMenu() {
		final var menu = new Menu("Aircraft Models >");
		menu.addItem(CREATE_AIRCRAFT_MODEL_OPTION, "Create Aircraft Model", () -> { new CreateAircraftModelUI().show(); return false; });
		menu.addItem(ADD_ENGINE_VARIANT_OPTION, "Add Engine Variant", () -> { new AddEngineVariantUI().show(); return false; });
		menu.addItem(REMOVE_ENGINE_VARIANT_OPTION, "Remove Engine Variant", () -> { new RemoveEngineVariantUI().show(); return false; });
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildAirControlAreasMenu() {
		final var menu = new Menu("Air Control Areas >");
		menu.addItem(REGISTER_ACA_OPTION, "Register Air Control Area", () -> { new RegisterAirControlAreaUI().show(); return false; });
		menu.addItem(LIST_ACA_OPTION, "List Air Control Areas", () -> { new ListAirControlAreasUI().show(); return false; });
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildAirportsMenu() {
		final var menu = new Menu("Airports >");
		menu.addItem(CREATE_AIRPORT_OPTION, "Create Airport", () -> { new CreateAirportUI().show(); return false; });
		menu.addItem(LIST_AIRPORTS_OPTION, "List Airports", () -> { new ListAirportsUI().show(); return false; });
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildCompaniesMenu() {
		final var menu = new Menu("Air Transport Companies >");
		menu.addItem(REGISTER_COMPANY_OPTION, "Register Air Transport Company", () -> { new RegisterAirTransportCompanyUI().show(); return false; });
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildCollaboratorsMenu() {
		final var menu = new Menu("Collaborators >");
		menu.addItem(ADD_COLLABORATOR_OPTION, "Add Collaborator", () -> { new AddCollaboratorUI().show(); return false; });
		menu.addItem(LIST_COLLABORATORS_OPTION, "List Collaborators", () -> { new ListCollaboratorsUI().show(); return false; });
		menu.addItem(EDIT_COLLABORATOR_OPTION, "Edit Collaborator", () -> { new EditCollaboratorUI().show(); return false; });
		menu.addItem(DISABLE_COLLABORATOR_OPTION, "Disable Collaborator", () -> { new DisableCollaboratorUI().show(); return false; });
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildAircraftMenu() {
		final var menu = new Menu("Aircraft >");
		menu.addItem(ADD_AIRCRAFT_OPTION, "Add Aircraft", () -> { new AddAircraftUI().show(); return false; });
		menu.addItem(LIST_FLEET_OPTION, "List Company Fleet", () -> { new ListCompanyFleetUI().show(); return false; });
		menu.addItem(DECOMMISSION_AIRCRAFT_OPTION, "Decommission Aircraft", () -> { new DecommissionAircraftUI().show(); return false; });
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildWeatherDataMenu() {
		final var menu = new Menu("Weather Data >");
		menu.addItem(REGISTER_WEATHER_DATA_OPTION, "Register Weather Data", () -> { new RegisterWeatherDataUI().show(); return false; });
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildAdminSettingsMenu() {
		final var menu = new Menu("Settings >");
		menu.addItem(SYSTEM_INFO_OPTION, "System Information", () -> {
			new ShowMessageAction("AISafe v" + Application.VERSION + "  |  " + Application.COPYRIGHT).execute();
			return false;
		});
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}
}
