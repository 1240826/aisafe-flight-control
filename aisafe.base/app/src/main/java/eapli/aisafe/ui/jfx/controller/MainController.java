package eapli.aisafe.ui.jfx.controller;

import eapli.aisafe.ui.jfx.SceneManager;
import eapli.aisafe.ui.jfx.controller.usecases.*;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class MainController {

    @FXML
    private AnchorPane contentArea;

    @FXML
    private Label userDisplayName;

    @FXML
    private Label userRole;

    @FXML
    private Label currentViewTitle;

    @FXML
    private VBox navItems;

    @FXML
    private VBox adminSection;

    @FXML
    private VBox backofficeSection;

    @FXML
    private VBox fleetSection;

    @FXML
    private VBox operationsSection;

    @FXML
    private VBox weatherSection;

    @FXML
    private VBox pilotSection;

    @FXML
    private VBox reportsSection;

    @FXML
    private void initialize() {
        userDisplayName.setText(SessionManager.currentDisplayName());
        userRole.setText(getRoleDisplay());

        NotificationManager.init(contentArea);

        showSectionIfAuthorized(adminSection, AISafeRoles.ADMIN);
        showSectionIfAuthorized(backofficeSection, AISafeRoles.BACKOFFICE_OPERATOR);
        showSectionIfAuthorized(fleetSection, AISafeRoles.ATC_COLLABORATOR);
        showSectionIfAuthorized(operationsSection, AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        showSectionIfAnyOf(weatherSection, AISafeRoles.WEATHER_PERSON, AISafeRoles.PILOT, AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        showSectionIfAuthorized(pilotSection, AISafeRoles.PILOT);
        showSectionIfAuthorized(reportsSection, AISafeRoles.FLIGHT_CONTROL_OPERATOR);

        contentArea.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) scene.setUserData(this);
        });

        showDashboard();
    }

    private void showSectionIfAuthorized(final VBox section, final eapli.framework.infrastructure.authz.domain.model.Role role) {
        final boolean visible = SessionManager.hasRole(role);
        section.setVisible(visible);
        section.setManaged(visible);
    }

    private void showSectionIfAnyOf(final VBox section, final eapli.framework.infrastructure.authz.domain.model.Role... roles) {
        boolean visible = false;
        for (final var role : roles) {
            if (SessionManager.hasRole(role)) { visible = true; break; }
        }
        section.setVisible(visible);
        section.setManaged(visible);
    }

    public void navigateFromChild(final String title, final String fxml, final Object controller) {
        currentViewTitle.setText(title);
        SceneManager.showInContentArea(fxml, () -> controller, contentArea);
    }

    private void navigate(final String title, final String fxml, final Object controller) {
        currentViewTitle.setText(title);
        SceneManager.showInContentArea(fxml, () -> controller, contentArea);
    }

    @FXML
    private void showDashboard() {
        navigate("Dashboard", "/fxml/Dashboard.fxml", new DashboardController());
    }

    @FXML
    private void showUsers() {
        navigate("User Management", "/fxml/usecases/Users.fxml", new UsersController());
    }

    @FXML
    private void showManufacturers() {
        navigate("Manufacturers", "/fxml/usecases/Manufacturer.fxml", new ManufacturerController());
    }

    @FXML
    private void showEngineModels() {
        navigate("Engine Models", "/fxml/usecases/EngineModel.fxml", new EngineModelController());
    }

    @FXML
    private void showAircraftModels() {
        navigate("Aircraft Models", "/fxml/usecases/AircraftModel.fxml", new AircraftModelController());
    }

    @FXML
    private void showAirControlAreas() {
        navigate("Air Control Areas", "/fxml/usecases/AirControlArea.fxml", new AirControlAreaController());
    }

    @FXML
    private void showAirports() {
        navigate("Airports", "/fxml/usecases/Airport.fxml", new AirportController());
    }

    @FXML
    private void showAirTransportCompanies() {
        navigate("Air Transport Companies", "/fxml/usecases/AirTransportCompany.fxml", new AirTransportCompanyController());
    }

    @FXML
    private void showCollaborators() {
        navigate("Collaborators", "/fxml/usecases/Collaborator.fxml", new CollaboratorController());
    }

    @FXML
    private void showAircraft() {
        navigate("Aircraft Fleet", "/fxml/usecases/Aircraft.fxml", new AircraftController());
    }

    @FXML
    private void showFlightRoutes() {
        navigate("Flight Routes", "/fxml/usecases/FlightRoute.fxml", new FlightRouteController());
    }

    @FXML
    private void showPilots() {
        navigate("Pilot Roster", "/fxml/usecases/Pilot.fxml", new PilotController());
    }

    @FXML
    private void showImportFlightPlan() {
        navigate("Import Flight Plan", "/fxml/usecases/ImportFlightPlan.fxml", new ImportFlightPlanController());
    }

    @FXML
    private void showTestFlightPlans() {
        navigate("Test Flight Plans", "/fxml/usecases/TestFlightPlans.fxml", new TestFlightPlansController());
    }

    @FXML
    private void showWeatherData() {
        navigate("Weather Data", "/fxml/usecases/WeatherData.fxml", new WeatherDataController());
    }

    @FXML
    private void showReports() {
        navigate("Monthly Reports", "/fxml/usecases/Reports.fxml", new ReportsController());
    }

    @FXML
    private void showSimulationReports() {
        navigate("Simulation Reports", "/fxml/usecases/SimulationReports.fxml", new SimulationReportController());
    }

    @FXML
    private void showSettings() {
        navigate("Settings", "/fxml/usecases/Settings.fxml", new SettingsController());
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        SceneManager.switchScene("AISafe Flight Control System", "/fxml/Login.fxml",
                () -> new LoginController());
    }

    private String getRoleDisplay() {
        return SessionManager.session()
                .map(s -> s.authenticatedUser())
                .map(u -> u.roleTypes().iterator().next().toString())
                .orElse("Guest");
    }
}
