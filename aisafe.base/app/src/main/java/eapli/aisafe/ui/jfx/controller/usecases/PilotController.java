package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.pilot.application.AddPilotController;
import eapli.aisafe.pilot.application.RemovePilotController;
import eapli.aisafe.pilot.application.ListPilotRosterController;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.aisafe.ui.jfx.util.FieldValidator;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SelectionMode;

import java.util.stream.StreamSupport;

public class PilotController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterCompany;

    @FXML
    private TableView<PilotRow> pilotsTable;

    @FXML
    private TableColumn<PilotRow, String> colPilotId;

    @FXML
    private TableColumn<PilotRow, String> colLicenseNumber;

    @FXML
    private TableColumn<PilotRow, String> colCompany;

    @FXML
    private TableColumn<PilotRow, String> colCertifiedModels;

    @FXML
    private TableColumn<PilotRow, String> colStatus;

    @FXML
    private TextField newLicenseNumber;

    @FXML
    private TextField newName;

    @FXML
    private ComboBox<String> newCompany;

    @FXML
    private ListView<String> certifiedModelsList;

    @FXML
    private Label newLicenseNumberError;

    @FXML
    private Label newNameError;

    @FXML
    private Label newCompanyError;

    private final AddPilotController addCtrl = new AddPilotController();
    private final ListPilotRosterController listCtrl = new ListPilotRosterController();
    private final RemovePilotController removeCtrl = new RemovePilotController();

    private final ObservableList<PilotRow> items = FXCollections.observableArrayList();
    private final ObservableList<String> availableModels = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colPilotId.setCellValueFactory(d -> d.getValue().id);
        colLicenseNumber.setCellValueFactory(d -> d.getValue().licenseNumber);
        colCompany.setCellValueFactory(d -> d.getValue().company);
        colCertifiedModels.setCellValueFactory(d -> d.getValue().certifiedModels);
        colStatus.setCellValueFactory(d -> d.getValue().status);

        FieldValidator.onRequired(newLicenseNumber, newLicenseNumberError, "License Number");
        FieldValidator.onPattern(newLicenseNumber, newLicenseNumberError, "[A-Z][0-9]{4,10}", "License must be a letter followed by 4-10 digits (e.g. P12345).");
        FieldValidator.onRequired(newName, newNameError, "Name");
        FieldValidator.onRequiredCombo(newCompany, newCompanyError, "Company");

        certifiedModelsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        loadCompanies();
        loadAircraftModels();
        refreshTable();
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

        filterCompany.valueProperty().addListener((o, a, b) -> refreshTable());
        searchField.textProperty().addListener((o, a, b) -> refreshTable());

        refreshTable();
    }

    private void loadAircraftModels() {
        try {
            availableModels.clear();
            StreamSupport.stream(addCtrl.allAircraftModels().spliterator(), false)
                    .forEach(m -> availableModels.add(m.identity().toString()));
            certifiedModelsList.setItems(availableModels);
        } catch (final Exception e) {
            NotificationManager.error("Error", "Could not load aircraft models: " + e.getMessage());
        }
    }

    @FXML
    private void refreshTable() {
        items.clear();
        final String companyFilter = filterCompany.getValue();
        final String searchText = searchField.getText();

        StreamSupport.stream(removeCtrl.allPilots().spliterator(), false)
                .filter(p -> companyFilter == null || "All".equals(companyFilter)
                        || p.company().toString().equals(companyFilter))
                .filter(p -> searchText == null || searchText.isBlank()
                        || p.identity().toString().toLowerCase().contains(searchText.toLowerCase())
                        || p.pilotId().toString().toLowerCase().contains(searchText.toLowerCase())
                        || p.company().toString().toLowerCase().contains(searchText.toLowerCase())
                        || p.certifiedModels().stream().anyMatch(m -> m.toString().toLowerCase().contains(searchText.toLowerCase())))
                .forEach(p -> items.add(new PilotRow(
                        p.pilotId().toString(),
                        p.pilotId().toString(),
                        p.company().toString(),
                        String.join(", ", p.certifiedModels().stream().map(Object::toString).toList()),
                        p.isActive() ? "Active" : "Inactive"
                )));
        pilotsTable.setItems(items);
    }

    @FXML
    private void addPilot() {
        if (!FieldValidator.isFormValid(newLicenseNumberError, newNameError, newCompanyError)) {
            NotificationManager.error("Validation Error", "Fix the highlighted fields before submitting.");
            return;
        }
        final var selectedModels = certifiedModelsList.getSelectionModel().getSelectedItems();
        if (selectedModels.isEmpty()) {
            NotificationManager.error("Validation Error", "Select at least one certified aircraft model.");
            return;
        }
        try {
            final var modelCodes = selectedModels.stream()
                    .map(eapli.aisafe.aircraftmodel.domain.AircraftModelCode::valueOf)
                    .collect(java.util.stream.Collectors.toSet());
            addCtrl.addPilot(newLicenseNumber.getText().trim().toUpperCase(),
                    eapli.aisafe.company.domain.CompanyIATA.valueOf(newCompany.getValue()),
                    modelCodes, LocalDate.now());
            NotificationManager.success("Pilot Added", "Pilot added successfully!");
            newLicenseNumber.clear();
            newName.clear();
            certifiedModelsList.getSelectionModel().clearSelection();
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void removePilot() {
        final var selected = pilotsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationManager.error("Selection Error", "Select a pilot to deactivate.");
            return;
        }
        try {
            removeCtrl.deactivatePilot(PilotId.valueOf(selected.id.get()));
            NotificationManager.success("Pilot Deactivated", "Pilot deactivated successfully.");
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void activatePilot() {
        final var selected = pilotsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationManager.error("Selection Error", "Select a pilot to activate.");
            return;
        }
        try {
            removeCtrl.activatePilot(PilotId.valueOf(selected.id.get()));
            NotificationManager.success("Pilot Activated", "Pilot activated successfully.");
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void onZoom() {
        TableZoomUtil.openZoom(pilotsTable, "Pilots");
    }

    public static class PilotRow {
        public final SimpleStringProperty id;
        public final SimpleStringProperty licenseNumber;
        public final SimpleStringProperty company;
        public final SimpleStringProperty certifiedModels;
        public final SimpleStringProperty status;

        public PilotRow(final String i, final String ln, final String c, final String cm, final String s) {
            id = new SimpleStringProperty(i);
            licenseNumber = new SimpleStringProperty(ln);
            company = new SimpleStringProperty(c);
            certifiedModels = new SimpleStringProperty(cm);
            status = new SimpleStringProperty(s);
        }
    }
}
