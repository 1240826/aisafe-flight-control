package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.collaborator.application.AddCollaboratorController;
import eapli.aisafe.collaborator.application.DisableCollaboratorController;
import eapli.aisafe.collaborator.application.ListCollaboratorsController;
import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.ui.jfx.util.FieldValidator;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.stream.StreamSupport;

public class CollaboratorController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterCustomer;

    @FXML
    private ComboBox<String> filterStatus;

    @FXML
    private TableView<CollaboratorRow> collaboratorsTable;

    @FXML
    private TableColumn<CollaboratorRow, String> colEmail;

    @FXML
    private TableColumn<CollaboratorRow, String> colName;

    @FXML
    private TableColumn<CollaboratorRow, String> colPhone;

    @FXML
    private TableColumn<CollaboratorRow, String> colCustomer;

    @FXML
    private TableColumn<CollaboratorRow, String> colStatus;

    @FXML
    private TextField newEmail;

    @FXML
    private TextField newName;

    @FXML
    private TextField newPhone;

    @FXML
    private ComboBox<String> newCustomer;

    @FXML
    private Label newEmailError;

    @FXML
    private Label newNameError;

    @FXML
    private Label newPhoneError;

    @FXML
    private Label newCustomerError;

    private final AddCollaboratorController addCtrl = new AddCollaboratorController();
    private final ListCollaboratorsController listCtrl = new ListCollaboratorsController();
    private final DisableCollaboratorController disableCtrl = new DisableCollaboratorController();

    private final ObservableList<CollaboratorRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colEmail.setCellValueFactory(d -> d.getValue().email);
        colName.setCellValueFactory(d -> d.getValue().name);
        colPhone.setCellValueFactory(d -> d.getValue().phone);
        colCustomer.setCellValueFactory(d -> d.getValue().customer);
        colStatus.setCellValueFactory(d -> d.getValue().status);

        FieldValidator.onRequired(newEmail, newEmailError, "Email");
        FieldValidator.onPattern(newEmail, newEmailError, ".+@.+\\..+", "Email must be valid (e.g. user@domain.com).");
        FieldValidator.onRequired(newName, newNameError, "Name");
        FieldValidator.onRequiredCombo(newCustomer, newCustomerError, "Customer");

        loadCustomers();
        filterStatus.getItems().addAll("All", "Active", "Inactive");
        filterStatus.getSelectionModel().selectFirst();

        filterCustomer.valueProperty().addListener((o, a, b) -> refreshTable());
        filterStatus.valueProperty().addListener((o, a, b) -> refreshTable());
        searchField.textProperty().addListener((o, a, b) -> refreshTable());

        refreshTable();
    }

    private void loadCustomers() {
        try {
            final var companies = listCtrl.allCompanies();
            StreamSupport.stream(companies.spliterator(), false)
                    .forEach(c -> {
                        newCustomer.getItems().add(c.iata().toString());
                        filterCustomer.getItems().add(c.iata().toString());
                    });
        } catch (final Exception e) {
            NotificationManager.error("Error", "Could not load companies: " + e.getMessage());
        }
        if (!newCustomer.getItems().isEmpty()) {
            newCustomer.getSelectionModel().selectFirst();
        }
        filterCustomer.getItems().add(0, "All");
        filterCustomer.getSelectionModel().selectFirst();
    }

    @FXML
    private void refreshTable() {
        items.clear();
        final String customerFilter = filterCustomer.getValue();
        final String statusFilter = filterStatus.getValue();
        final String searchText = searchField.getText();

        StreamSupport.stream(listCtrl.allActiveCollaborators().spliterator(), false)
                .filter(c -> customerFilter == null || "All".equals(customerFilter)
                        || (c.companyId() != null && c.companyId().toString().equals(customerFilter)))
                .filter(c -> statusFilter == null || "All".equals(statusFilter)
                        || (c.isActive() ? "Active" : "Inactive").equalsIgnoreCase(statusFilter))
                .filter(c -> searchText == null || searchText.isBlank()
                        || c.identity().toString().toLowerCase().contains(searchText.toLowerCase())
                        || c.name().toLowerCase().contains(searchText.toLowerCase())
                        || (c.companyId() != null && c.companyId().toString().toLowerCase().contains(searchText.toLowerCase())))
                .forEach(c -> items.add(new CollaboratorRow(
                        c.identity().toString(),
                        c.name(),
                        c.phone() != null ? c.phone() : "N/A",
                        c.companyId() != null ? c.companyId().toString() : "—",
                        c.isActive() ? "Active" : "Inactive"
                )));
        collaboratorsTable.setItems(items);
    }

    @FXML
    private void addCollaborator() {
        if (!FieldValidator.isFormValid(newEmailError, newNameError, newCustomerError)) {
            NotificationManager.error("Validation Error", "Fix the highlighted fields before submitting.");
            return;
        }
        try {
            addCtrl.addATCCollaborator(
                    newEmail.getText(), "TempPass123", "First", "Last",
                    newEmail.getText(), newName.getText(), "ATC",
                    LocalDate.now().plusYears(5),
                    LocalDate.now().plusYears(3),
                    newCustomer.getValue()
            );
            NotificationManager.success("Collaborator Added", "Collaborator added successfully!");
            newEmail.clear();
            newName.clear();
            newPhone.clear();
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void disableCollaborator() {
        final var selected = collaboratorsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationManager.error("Selection Error", "Select a collaborator to disable.");
            return;
        }
        try {
            final var email = selected.email.get();
            NotificationManager.info("Disable", "Use console for disable operation: " + email);
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void onZoom() {
        TableZoomUtil.openZoom(collaboratorsTable, "Collaborators");
    }

    public static class CollaboratorRow {
        public final SimpleStringProperty email;
        public final SimpleStringProperty name;
        public final SimpleStringProperty phone;
        public final SimpleStringProperty customer;
        public final SimpleStringProperty status;

        public CollaboratorRow(final String e, final String n, final String p,
                               final String c, final String s) {
            email = new SimpleStringProperty(e);
            name = new SimpleStringProperty(n);
            phone = new SimpleStringProperty(p);
            customer = new SimpleStringProperty(c);
            status = new SimpleStringProperty(s);
        }
    }
}
