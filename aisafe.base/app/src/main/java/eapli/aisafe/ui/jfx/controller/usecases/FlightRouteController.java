package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.airport.application.CreateAirportController;
import eapli.aisafe.flightroute.application.CreateFlightRouteController;
import eapli.aisafe.flightroute.application.DeleteFlightRouteController;
import eapli.aisafe.ui.jfx.util.ConfirmationDialog;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.RouteMapUtil;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.stream.StreamSupport;

public class FlightRouteController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterCompany;

    @FXML
    private ComboBox<String> filterStatus;

    @FXML
    private TableView<RouteRow> routesTable;

    @FXML
    private TableColumn<RouteRow, String> colName;

    @FXML
    private TableColumn<RouteRow, String> colOrigin;

    @FXML
    private TableColumn<RouteRow, String> colDestination;

    @FXML
    private TableColumn<RouteRow, String> colCompany;

    @FXML
    private TableColumn<RouteRow, String> colStatus;

    @FXML
    private TextField newName;

    @FXML
    private ComboBox<String> newOrigin;

    @FXML
    private ComboBox<String> newDestination;

    @FXML
    private ComboBox<String> newCompany;

    private final CreateFlightRouteController ctrl = new CreateFlightRouteController();
    private final DeleteFlightRouteController delCtrl = new DeleteFlightRouteController();
    private final ObservableList<RouteRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colName.setCellValueFactory(d -> d.getValue().name);
        colOrigin.setCellValueFactory(d -> d.getValue().origin);
        colDestination.setCellValueFactory(d -> d.getValue().destination);
        colCompany.setCellValueFactory(d -> d.getValue().company);
        colStatus.setCellValueFactory(d -> d.getValue().status);

        loadAirports();
        loadCompanies();
        filterStatus.getItems().addAll("All", "Active", "Inactive");
        filterStatus.getSelectionModel().selectFirst();
        refreshTable();
    }

    private void loadAirports() {
        try {
            final var airports = new eapli.aisafe.airport.application
                    .CreateAirportController().allAirports();
            StreamSupport.stream(airports.spliterator(), false)
                    .forEach(a -> {
                        newOrigin.getItems().add(a.identity().toString());
                        newDestination.getItems().add(a.identity().toString());
                    });
        } catch (final Exception e) {
            NotificationManager.error("Error", "Could not load airports: " + e.getMessage());
        }
        if (!newOrigin.getItems().isEmpty()) {
            newOrigin.getSelectionModel().selectFirst();
        }
        if (!newDestination.getItems().isEmpty()) {
            newDestination.getSelectionModel().selectFirst();
        }
    }

    private void loadCompanies() {
        try {
            final var companies = new eapli.aisafe.company.application
                    .RegisterAirTransportCompanyController().allCompanies();
            StreamSupport.stream(companies.spliterator(), false)
                    .forEach(c -> {
                        newCompany.getItems().add(c.iata().toString());
                        filterCompany.getItems().add(c.iata().toString());
                    });
        } catch (final Exception e) {
            NotificationManager.error("Error", "Could not load companies: " + e.getMessage());
        }
        if (!newCompany.getItems().isEmpty()) {
            newCompany.getSelectionModel().selectFirst();
        }
        filterCompany.getItems().add(0, "All");
        filterCompany.getSelectionModel().selectFirst();
    }

    @FXML
    private void refreshTable() {
        items.clear();
        final String companyFilter = filterCompany.getValue();
        final String statusFilter = filterStatus.getValue();
        final String searchText = searchField.getText();

        StreamSupport.stream(ctrl.allActiveRoutes().spliterator(), false)
                .filter(r -> companyFilter == null || "All".equals(companyFilter)
                        || r.companyIATA().toString().equals(companyFilter))
                .filter(r -> statusFilter == null || "All".equals(statusFilter)
                        || (r.isActive() ? "Active" : "Inactive").equalsIgnoreCase(statusFilter))
                .filter(r -> searchText == null || searchText.isBlank()
                        || r.identity().toString().toLowerCase().contains(searchText.toLowerCase())
                        || r.origin().toString().toLowerCase().contains(searchText.toLowerCase())
                        || r.destination().toString().toLowerCase().contains(searchText.toLowerCase()))
                .forEach(r -> items.add(new RouteRow(
                        r.identity().toString(),
                        r.origin().toString(),
                        r.destination().toString(),
                        r.companyIATA().toString(),
                        r.isActive() ? "Active" : "Inactive"
                )));
        routesTable.setItems(items);
    }

    @FXML
    private void addRoute() {
        try {
            if (newName.getText().isBlank()) {
                NotificationManager.error("Validation Error", "Route name is required.");
                return;
            }
            ctrl.createFlightRoute(
                    newName.getText(),
                    newCompany.getValue(),
                    newOrigin.getValue(),
                    newDestination.getValue()
            );
            NotificationManager.success("Route Created", "Flight route created!");
            newName.clear();
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void deleteRoute() {
        final var selected = routesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationManager.error("Selection Error", "Select a route to delete.");
            return;
        }
        if (!ConfirmationDialog.confirm("Delete Route",
                "Are you sure you want to delete route " + selected.name.get() + "?")) {
            return;
        }
        try {
            delCtrl.deactivateRoute(selected.name.get(), LocalDate.now());
            NotificationManager.success("Route Deleted", "Route deleted successfully.");
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void viewOnMap() {
        final var selected = routesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationManager.error("Selection", "Select a route to view on map.");
            return;
        }
        try {
            final Map<String, double[]> coords = new HashMap<>();
            final Map<String, String> names = new HashMap<>();
            final var airports = new CreateAirportController().allAirports();
            StreamSupport.stream(airports.spliterator(), false).forEach(a -> {
                coords.put(a.identity().toString(), new double[]{a.latitude(), a.longitude()});
                names.put(a.identity().toString(), a.name());
            });

            final String originCode = selected.origin.get();
            final String destCode = selected.destination.get();
            final double[] originCoord = coords.getOrDefault(originCode, new double[]{0, 0});
            final double[] destCoord = coords.getOrDefault(destCode, new double[]{0, 0});

            RouteMapUtil.showRouteMap(
                    selected.name.get(),
                    originCode, names.getOrDefault(originCode, ""),
                    originCoord[0], originCoord[1],
                    destCode, names.getOrDefault(destCode, ""),
                    destCoord[0], destCoord[1]
            );
        } catch (final Exception e) {
            NotificationManager.error("Error", "Could not load map: " + e.getMessage());
        }
    }

    @FXML
    private void onZoom() {
        TableZoomUtil.openZoom(routesTable, "Flight Routes");
    }

    public static class RouteRow {
        public final SimpleStringProperty name;
        public final SimpleStringProperty origin;
        public final SimpleStringProperty destination;
        public final SimpleStringProperty company;
        public final SimpleStringProperty status;

        public RouteRow(final String n, final String o, final String d,
                        final String c, final String s) {
            name = new SimpleStringProperty(n);
            origin = new SimpleStringProperty(o);
            destination = new SimpleStringProperty(d);
            company = new SimpleStringProperty(c);
            status = new SimpleStringProperty(s);
        }
    }
}
