package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.aircraft.application.AddAircraftController;
import eapli.aisafe.aircraft.application.DecommissionAircraftController;
import eapli.aisafe.aircraft.application.ListCompanyFleetController;
import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.StreamSupport;

public class AircraftController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterModel;

    @FXML
    private ComboBox<String> filterCompany;

    @FXML
    private ComboBox<String> filterStatus;

    @FXML
    private TableView<AircraftRow> aircraftTable;

    @FXML
    private TableColumn<AircraftRow, String> colReg;

    @FXML
    private TableColumn<AircraftRow, String> colModel;

    @FXML
    private TableColumn<AircraftRow, String> colCompany;

    @FXML
    private TableColumn<AircraftRow, String> colCrew;

    @FXML
    private TableColumn<AircraftRow, String> colCapacity;

    @FXML
    private TableColumn<AircraftRow, String> colStatus;

    @FXML
    private TextField newReg;

    @FXML
    private TextField newRegCountry;

    @FXML
    private ComboBox<String> newModel;

    @FXML
    private ComboBox<String> newCompany;

    @FXML
    private TextField newCrew;

    @FXML
    private TextField newSeats;

    private final AddAircraftController addCtrl = new AddAircraftController();
    private final ListCompanyFleetController listCtrl = new ListCompanyFleetController();
    private final DecommissionAircraftController decommCtrl = new DecommissionAircraftController();
    private final ObservableList<AircraftRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colReg.setCellValueFactory(d -> d.getValue().registration);
        colModel.setCellValueFactory(d -> d.getValue().model);
        colCompany.setCellValueFactory(d -> d.getValue().company);
        colCrew.setCellValueFactory(d -> d.getValue().crew);
        colCapacity.setCellValueFactory(d -> d.getValue().capacity);
        colStatus.setCellValueFactory(d -> d.getValue().status);

        loadModels();
        loadCompanies();

        filterModel.getItems().add("All");
        filterCompany.getItems().add("All");
        filterStatus.getItems().addAll("All", "Active", "Decommissioned");
        filterModel.getSelectionModel().selectFirst();
        filterCompany.getSelectionModel().selectFirst();
        filterStatus.getSelectionModel().selectFirst();

        refreshTable();
    }

    private void loadModels() {
        try {
            final var ctrl = new eapli.aisafe.aircraftmodel.application.CreateAircraftModelController();
            StreamSupport.stream(ctrl.allAircraftModels().spliterator(), false)
                    .forEach(m -> {
                        newModel.getItems().add(m.identity().toString());
                        filterModel.getItems().add(m.identity().toString());
                    });
        } catch (final Exception e) {
            NotificationManager.error("Error", "Could not load models: " + e.getMessage());
        }
    }

    private void loadCompanies() {
        try {
            final var ctrl = new eapli.aisafe.company.application.RegisterAirTransportCompanyController();
            StreamSupport.stream(ctrl.allCompanies().spliterator(), false)
                    .forEach(c -> {
                        newCompany.getItems().add(c.identity().toString());
                        filterCompany.getItems().add(c.identity().toString());
                    });
        } catch (final Exception e) {
            NotificationManager.error("Error", "Could not load companies: " + e.getMessage());
        }
    }

    @FXML
    private void refreshTable() {
        items.clear();
        try {
            final var fleet = listCtrl.allActiveAircraft();
            final String modelFilter = filterModel.getValue();
            final String companyFilter = filterCompany.getValue();
            final String statusFilter = filterStatus.getValue();
            final String searchText = searchField.getText();

            StreamSupport.stream(fleet.spliterator(), false)
                    .filter(a -> modelFilter == null || "All".equals(modelFilter)
                            || a.aircraftModelCode().toString().equals(modelFilter))
                    .filter(a -> companyFilter == null || "All".equals(companyFilter)
                            || a.companyId().toString().equals(companyFilter))
                    .filter(a -> statusFilter == null || "All".equals(statusFilter)
                            || a.operationalStatus().toString().equalsIgnoreCase(statusFilter))
                    .filter(a -> searchText == null || searchText.isBlank()
                            || a.identity().toString().toLowerCase().contains(searchText.toLowerCase())
                            || a.aircraftModelCode().toString().toLowerCase().contains(searchText.toLowerCase())
                            || a.companyId().toString().toLowerCase().contains(searchText.toLowerCase()))
                    .forEach(a -> items.add(new AircraftRow(a)));
        } catch (final Exception e) {
            NotificationManager.error("Error", "Could not load fleet: " + e.getMessage());
        }
        aircraftTable.setItems(items);
    }

    @FXML
    private void addAircraft() {
        try {
            if (newReg.getText().isBlank() || newModel.getValue() == null || newCompany.getValue() == null) {
                NotificationManager.error("Validation Error", "Registration, Model, and Company are required.");
                return;
            }
            final int seats = newSeats.getText().isBlank() ? 0 : Integer.parseInt(newSeats.getText());
            final int crew = newCrew.getText().isBlank() ? 2 : Integer.parseInt(newCrew.getText());
            final String regCountry = newRegCountry.getText().isBlank() ? "Unknown" : newRegCountry.getText();

            final var seatClass = new SeatClass("Economy", seats);
            addCtrl.addAircraft(newReg.getText(), regCountry, newModel.getValue(),
                    newCompany.getValue(), crew, List.of(seatClass), LocalDate.now());
            NotificationManager.success("Aircraft Added", newReg.getText() + " registered successfully.");
            newReg.clear(); newRegCountry.clear(); newCrew.clear(); newSeats.clear();
            refreshTable();
        } catch (final NumberFormatException e) {
            NotificationManager.error("Validation Error", "Crew and Seats must be valid numbers.");
        } catch (final Exception e) {
            NotificationManager.error("Registration Failed", e.getMessage());
        }
    }

    @FXML
    private void onZoom() {
        TableZoomUtil.openZoom(aircraftTable, "Aircraft Fleet");
    }

    public static class AircraftRow {
        public final SimpleStringProperty registration;
        public final SimpleStringProperty model;
        public final SimpleStringProperty company;
        public final SimpleStringProperty crew;
        public final SimpleStringProperty capacity;
        public final SimpleStringProperty status;

        public AircraftRow(final Aircraft a) {
            registration = new SimpleStringProperty(a.identity().toString());
            model = new SimpleStringProperty(a.aircraftModelCode().toString());
            company = new SimpleStringProperty(a.companyId().toString());
            crew = new SimpleStringProperty(String.valueOf(a.numberOfFlightCrewMembers()));
            capacity = new SimpleStringProperty(String.valueOf(a.totalCapacity()));
            status = new SimpleStringProperty(a.operationalStatus().toString());
        }
    }
}
