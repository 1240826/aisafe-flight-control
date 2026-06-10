package eapli.aisafe.ui.jfx.controller;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.ui.jfx.controller.usecases.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML
    private Label totalAircraft;

    @FXML
    private Label totalAirports;

    @FXML
    private Label totalCompanies;

    @FXML
    private Label totalFlightPlans;

    @FXML
    private Label activityUser;

    @FXML
    private Label systemRuntime;

    @FXML
    private void initialize() {
        try {
            final var aircraftRepo = PersistenceContext.repositories().aircraft();
            final var airportRepo = PersistenceContext.repositories().airports();
            final var companyRepo = PersistenceContext.repositories().airTransportCompanies();
            final var flightPlanRepo = PersistenceContext.repositories().flights();

            totalAircraft.setText(String.valueOf(countIterable(aircraftRepo.findAll())));
            totalAirports.setText(String.valueOf(countIterable(airportRepo.findAll())));
            totalCompanies.setText(String.valueOf(countIterable(companyRepo.findAll())));
            totalFlightPlans.setText(String.valueOf(countIterable(flightPlanRepo.findAll())));
        } catch (final Exception e) {
            totalAircraft.setText("--");
            totalAirports.setText("--");
            totalCompanies.setText("--");
            totalFlightPlans.setText("--");
        }
        activityUser.setText("Logged in as " + SessionManager.currentDisplayName());
        systemRuntime.setText(System.getProperty("java.version", "?"));
    }

    @FXML
    private void onQuickAirport() {
        navigate("Airports", "/fxml/usecases/Airport.fxml", new AirportController());
    }

    @FXML
    private void onQuickAircraft() {
        navigate("Aircraft Fleet", "/fxml/usecases/Aircraft.fxml", new AircraftController());
    }

    @FXML
    private void onQuickRoute() {
        navigate("Flight Routes", "/fxml/usecases/FlightRoute.fxml", new FlightRouteController());
    }

    @FXML
    private void onQuickFlightPlan() {
        navigate("Import Flight Plan", "/fxml/usecases/ImportFlightPlan.fxml", new ImportFlightPlanController());
    }

    @FXML
    private void onQuickWeather() {
        navigate("Weather Data", "/fxml/usecases/WeatherData.fxml", new WeatherDataController());
    }

    private void navigate(final String title, final String fxml, final Object controller) {
        final var mainController = (MainController) totalAircraft.getScene().getUserData();
        if (mainController != null) {
            mainController.navigateFromChild(title, fxml, controller);
        } else {
            eapli.aisafe.ui.jfx.SceneManager.showInContentArea(fxml, () -> controller,
                    (javafx.scene.layout.AnchorPane) totalAircraft.getScene().lookup("#contentArea"));
        }
    }

    private long countIterable(final Iterable<?> iterable) {
        long count = 0;
        for (final var ignored : iterable) count++;
        return count;
    }
}
