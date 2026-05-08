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
package eapli.exemplo.app.backoffice.console.presentation;

import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.app.backoffice.console.presentation.aircontrolarea.ListAirControlAreasUI;
import eapli.aisafe.app.backoffice.console.presentation.aircontrolarea.RegisterAirControlAreaUI;
import eapli.aisafe.app.backoffice.console.presentation.aircraft.AddAircraftUI;
import eapli.aisafe.app.backoffice.console.presentation.aircraft.DecommissionAircraftUI;
import eapli.aisafe.app.backoffice.console.presentation.aircraft.ListCompanyFleetUI;
import eapli.aisafe.app.backoffice.console.presentation.aircraftmodel.AddEngineVariantUI;
import eapli.aisafe.app.backoffice.console.presentation.aircraftmodel.CreateAircraftModelUI;
import eapli.aisafe.app.backoffice.console.presentation.aircraftmodel.RemoveEngineVariantUI;
import eapli.aisafe.app.backoffice.console.presentation.airport.CreateAirportUI;
import eapli.aisafe.app.backoffice.console.presentation.airport.ListAirportsUI;
import eapli.aisafe.app.backoffice.console.presentation.collaborator.AddCollaboratorUI;
import eapli.aisafe.app.backoffice.console.presentation.collaborator.DisableCollaboratorUI;
import eapli.aisafe.app.backoffice.console.presentation.collaborator.EditCollaboratorUI;
import eapli.aisafe.app.backoffice.console.presentation.collaborator.ListCollaboratorsUI;
import eapli.aisafe.app.backoffice.console.presentation.company.RegisterAirTransportCompanyUI;
import eapli.aisafe.app.backoffice.console.presentation.enginemodel.CreateEngineModelUI;
import eapli.aisafe.app.backoffice.console.presentation.manufacturer.RegisterManufacturerUI;
import eapli.aisafe.app.backoffice.console.presentation.weatherdata.RegisterWeatherDataUI;
import eapli.exemplo.Application;
import eapli.exemplo.app.backoffice.console.presentation.authz.ActivateUserUI;
import eapli.exemplo.app.backoffice.console.presentation.authz.AddUserUI;
import eapli.exemplo.app.backoffice.console.presentation.authz.DeactivateUserAction;
import eapli.exemplo.app.backoffice.console.presentation.authz.ListUsersAction;
import eapli.exemplo.app.backoffice.console.presentation.utente.AcceptRefuseSignupRequestAction;
import eapli.exemplo.app.common.console.presentation.authz.MyUserMenu;
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
	private static final int ACCEPT_REFUSE_SIGNUP_REQUEST_OPTION = 5;

	// SETTINGS submenu
	private static final int SET_KITCHEN_ALERT_LIMIT_OPTION = 1;

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

	private Menu buildMainMenu() {
		final var mainMenu = new Menu();
		final Menu myUserMenu = new MyUserMenu();
		mainMenu.addSubMenu(MY_USER_OPTION, myUserMenu);

		if (!Application.settings().isMenuLayoutHorizontal()) {
			mainMenu.addItem(MenuItem.separator(SEPARATOR_LABEL));
		}

		if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.ADMIN)) {
			// US031/032/033 — User management
			mainMenu.addSubMenu(USERS_OPTION, buildUsersMenu());
			// US010 — Manufacturers
			mainMenu.addSubMenu(MANUFACTURERS_OPTION, buildManufacturersMenu());
			// US020 — Engine Models
			mainMenu.addSubMenu(ENGINE_MODELS_OPTION, buildEngineModelsMenu());
			// US030 — Aircraft Models
			mainMenu.addSubMenu(AIRCRAFT_MODELS_OPTION, buildAircraftModelsMenu());
			// US050/051 — Air Control Areas
			mainMenu.addSubMenu(ACA_OPTION, buildAirControlAreasMenu());
			// US060 — Airports
			mainMenu.addSubMenu(AIRPORTS_OPTION, buildAirportsMenu());
			// Companies
			mainMenu.addSubMenu(COMPANIES_OPTION, buildCompaniesMenu());
			// US061/062/063/064 — Collaborators
			mainMenu.addSubMenu(COLLABORATORS_OPTION, buildCollaboratorsMenu());
			// Aircraft
			mainMenu.addSubMenu(AIRCRAFT_OPTION, buildAircraftMenu());
			// US041 — Weather Data
			mainMenu.addSubMenu(WEATHER_DATA_OPTION, buildWeatherDataMenu());
			// Settings
			mainMenu.addSubMenu(SETTINGS_OPTION, buildAdminSettingsMenu());
		}

		if (!Application.settings().isMenuLayoutHorizontal()) {
			mainMenu.addItem(MenuItem.separator(SEPARATOR_LABEL));
		}

		mainMenu.addItem(EXIT_OPTION, "Exit", new ExitWithMessageAction("Bye, Bye"));
		return mainMenu;
	}

	private Menu buildUsersMenu() {
		final var menu = new Menu("Users >");
		menu.addItem(ADD_USER_OPTION, "Add User", new AddUserUI()::show);
		menu.addItem(LIST_USERS_OPTION, "List all Users", new ListUsersAction());
		menu.addItem(DEACTIVATE_USER_OPTION, "Disable User", new DeactivateUserAction());
		menu.addItem(ACTIVATE_USER_OPTION, "Enable User", new ActivateUserUI()::show);
		menu.addItem(ACCEPT_REFUSE_SIGNUP_REQUEST_OPTION, "Accept/Refuse Signup Request",
				new AcceptRefuseSignupRequestAction());
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildManufacturersMenu() {
		final var menu = new Menu("Manufacturers >");
		menu.addItem(REGISTER_MANUFACTURER_OPTION, "Register Manufacturer", new RegisterManufacturerUI()::show);
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildEngineModelsMenu() {
		final var menu = new Menu("Engine Models >");
		menu.addItem(CREATE_ENGINE_MODEL_OPTION, "Create Engine Model", new CreateEngineModelUI()::show);
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildAircraftModelsMenu() {
		final var menu = new Menu("Aircraft Models >");
		menu.addItem(CREATE_AIRCRAFT_MODEL_OPTION, "Create Aircraft Model", new CreateAircraftModelUI()::show);
		menu.addItem(ADD_ENGINE_VARIANT_OPTION, "Add Engine Variant", new AddEngineVariantUI()::show);
		menu.addItem(REMOVE_ENGINE_VARIANT_OPTION, "Remove Engine Variant", new RemoveEngineVariantUI()::show);
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildAirControlAreasMenu() {
		final var menu = new Menu("Air Control Areas >");
		menu.addItem(REGISTER_ACA_OPTION, "Register Air Control Area", new RegisterAirControlAreaUI()::show);
		menu.addItem(LIST_ACA_OPTION, "List Air Control Areas", new ListAirControlAreasUI()::show);
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildAirportsMenu() {
		final var menu = new Menu("Airports >");
		menu.addItem(CREATE_AIRPORT_OPTION, "Create Airport", new CreateAirportUI()::show);
		menu.addItem(LIST_AIRPORTS_OPTION, "List Airports", new ListAirportsUI()::show);
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildCompaniesMenu() {
		final var menu = new Menu("Air Transport Companies >");
		menu.addItem(REGISTER_COMPANY_OPTION, "Register Air Transport Company", new RegisterAirTransportCompanyUI()::show);
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildCollaboratorsMenu() {
		final var menu = new Menu("Collaborators >");
		menu.addItem(ADD_COLLABORATOR_OPTION, "Add Collaborator", new AddCollaboratorUI()::show);
		menu.addItem(LIST_COLLABORATORS_OPTION, "List Collaborators", new ListCollaboratorsUI()::show);
		menu.addItem(EDIT_COLLABORATOR_OPTION, "Edit Collaborator", new EditCollaboratorUI()::show);
		menu.addItem(DISABLE_COLLABORATOR_OPTION, "Disable Collaborator", new DisableCollaboratorUI()::show);
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildAircraftMenu() {
		final var menu = new Menu("Aircraft >");
		menu.addItem(ADD_AIRCRAFT_OPTION, "Add Aircraft", new AddAircraftUI()::show);
		menu.addItem(LIST_FLEET_OPTION, "List Company Fleet", new ListCompanyFleetUI()::show);
		menu.addItem(DECOMMISSION_AIRCRAFT_OPTION, "Decommission Aircraft", new DecommissionAircraftUI()::show);
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildWeatherDataMenu() {
		final var menu = new Menu("Weather Data >");
		menu.addItem(REGISTER_WEATHER_DATA_OPTION, "Register Weather Data", new RegisterWeatherDataUI()::show);
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildAdminSettingsMenu() {
		final var menu = new Menu("Settings >");
		menu.addItem(SET_KITCHEN_ALERT_LIMIT_OPTION, "Set kitchen alert limit",
				new ShowMessageAction("Not implemented yet"));
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}
}
