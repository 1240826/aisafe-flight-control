package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.pilot.application.AddPilotController;
import eapli.aisafe.pilot.application.RemovePilotController;
import eapli.aisafe.pilot.application.ListPilotRosterController;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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
    private TableColumn<PilotRow, String> colEmail;

    @FXML
    private TableColumn<PilotRow, String> colCompany;

    @FXML
    private TableColumn<PilotRow, String> colStatus;

    @FXML
    private TextField newEmail;

    @FXML
    private TextField newName;

    @FXML
    private ComboBox<String> newCompany;

    private final AddPilotController addCtrl = new AddPilotController();
    private final ListPilotRosterController listCtrl = new ListPilotRosterController();
    private final RemovePilotController removeCtrl = new RemovePilotController();

    private final ObservableList<PilotRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colPilotId.setCellValueFactory(d -> d.getValue().id);
        colEmail.setCellValueFactory(d -> d.getValue().email);
        colCompany.setCellValueFactory(d -> d.getValue().company);
        colStatus.setCellValueFactory(d -> d.getValue().status);

        loadCompanies();
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
                        || p.company().toString().toLowerCase().contains(searchText.toLowerCase()))
                .forEach(p -> items.add(new PilotRow(
                        p.pilotId().toString(),
                        p.identity().toString(),
                        p.company().toString(),
                        p.isActive() ? "Active" : "Inactive"
                )));
        pilotsTable.setItems(items);
    }

    @FXML
    private void addPilot() {
        try {
            if (newEmail.getText().isBlank() || newName.getText().isBlank()) {
                NotificationManager.error("Validation Error", "Email and name are required.");
                return;
            }
            addCtrl.addPilot(newEmail.getText(),
                    eapli.aisafe.company.domain.CompanyIATA.valueOf(newCompany.getValue()),
                    Set.of(), LocalDate.now());
            NotificationManager.success("Pilot Added", "Pilot added successfully!");
            newEmail.clear();
            newName.clear();
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void removePilot() {
        final var selected = pilotsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationManager.error("Selection Error", "Select a pilot to remove.");
            return;
        }
        try {
            removeCtrl.deactivatePilot(PilotId.valueOf(selected.id.get()));
            NotificationManager.success("Pilot Removed", "Pilot removed.");
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
        public final SimpleStringProperty email;
        public final SimpleStringProperty company;
        public final SimpleStringProperty status;

        public PilotRow(final String i, final String e, final String c, final String s) {
            id = new SimpleStringProperty(i);
            email = new SimpleStringProperty(e);
            company = new SimpleStringProperty(c);
            status = new SimpleStringProperty(s);
        }
    }
}
