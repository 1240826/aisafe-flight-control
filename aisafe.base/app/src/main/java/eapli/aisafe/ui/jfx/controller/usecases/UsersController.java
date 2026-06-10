package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.ui.jfx.controller.SessionManager;
import eapli.aisafe.ui.jfx.util.ConfirmationDialog;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import eapli.aisafe.usermanagement.application.AddUserController;
import eapli.aisafe.usermanagement.application.DeactivateUserController;
import eapli.aisafe.usermanagement.application.ListUsersController;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class UsersController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterRole;

    @FXML
    private ComboBox<String> filterStatus;

    @FXML
    private TableView<UserRow> usersTable;

    @FXML
    private TableColumn<UserRow, String> colUsername;

    @FXML
    private TableColumn<UserRow, String> colDisplayName;

    @FXML
    private TableColumn<UserRow, String> colEmail;

    @FXML
    private TableColumn<UserRow, String> colRoles;

    @FXML
    private TableColumn<UserRow, String> colStatus;

    @FXML
    private TextField newUsername;

    @FXML
    private TextField newDisplayName;

    @FXML
    private TextField newEmail;

    @FXML
    private PasswordField newPassword;

    @FXML
    private ComboBox<String> newRole;

    private final AddUserController addCtrl = new AddUserController();
    private final ListUsersController listCtrl = new ListUsersController();
    private final DeactivateUserController deactCtrl = new DeactivateUserController();

    private final ObservableList<UserRow> users = FXCollections.observableArrayList();
    private final java.util.ArrayList<SystemUser> systemUsers = new java.util.ArrayList<>();

    @FXML
    private void initialize() {
        colUsername.setCellValueFactory(d -> d.getValue().username);
        colDisplayName.setCellValueFactory(d -> d.getValue().displayName);
        colEmail.setCellValueFactory(d -> d.getValue().email);
        colRoles.setCellValueFactory(d -> d.getValue().roles);
        colStatus.setCellValueFactory(d -> d.getValue().status);

        newRole.getItems().addAll("ADMIN", "BACKOFFICE_OPERATOR",
                "ATC_COLLABORATOR", "FLIGHT_CONTROL_OPERATOR",
                "WEATHER_PERSON", "PILOT");
        newRole.getSelectionModel().selectFirst();

        filterRole.getItems().addAll("All", "ADMIN", "BACKOFFICE_OPERATOR",
                "ATC_COLLABORATOR", "FLIGHT_CONTROL_OPERATOR",
                "WEATHER_PERSON", "PILOT");
        filterRole.getSelectionModel().selectFirst();
        filterStatus.getItems().addAll("All", "Active", "Inactive");
        filterStatus.getSelectionModel().selectFirst();

        refreshTable();
    }

    @FXML
    private void refreshTable() {
        users.clear();
        systemUsers.clear();
        try {
            listCtrl.allUsers().forEach(u -> {
                systemUsers.add(u);
                users.add(new UserRow(
                        u.username().toString(),
                        u.name().toString(),
                        u.email().toString(),
                        String.join(", ", u.roleTypes().stream()
                                .map(Object::toString).toList()),
                        u.isActive() ? "Active" : "Inactive"
                ));
            });
            usersTable.setItems(users);
        } catch (final Exception e) {
            NotificationManager.error("Error", "Could not load users: " + e.getMessage());
        }
    }

    @FXML
    private void addUser() {
        try {
            final var username = newUsername.getText();
            final var displayName = newDisplayName.getText();
            final var email = newEmail.getText();
            final var password = newPassword.getText();
            final var roleStr = newRole.getValue();

            if (username.isBlank() || displayName.isBlank() || email.isBlank() || password.isBlank()) {
                NotificationManager.error("Validation Error", "All fields are required.");
                return;
            }

            addCtrl.addUser(username, password, displayName, "", email,
                    java.util.Set.of(Role.valueOf(roleStr)),
                    java.time.LocalDate.now().plusYears(1));

            NotificationManager.success("User Created", "User created successfully!");
            clearForm();
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void disableUser() {
        final var selectedIdx = usersTable.getSelectionModel().getSelectedIndex();
        if (selectedIdx < 0 || selectedIdx >= systemUsers.size()) {
            NotificationManager.error("Selection Error", "Select a user to disable.");
            return;
        }
        if (!ConfirmationDialog.confirm("Disable User",
                "Are you sure you want to disable user " + systemUsers.get(selectedIdx).username() + "?")) {
            return;
        }
        try {
            deactCtrl.deactivateUser(systemUsers.get(selectedIdx));
            NotificationManager.success("User Disabled", "User disabled successfully.");
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void enableUser() {
        final var selectedIdx = usersTable.getSelectionModel().getSelectedIndex();
        if (selectedIdx < 0 || selectedIdx >= systemUsers.size()) {
            NotificationManager.error("Selection Error", "Select a user to enable.");
            return;
        }
        if (!ConfirmationDialog.confirm("Enable User",
                "Are you sure you want to enable user " + systemUsers.get(selectedIdx).username() + "?")) {
            return;
        }
        try {
            deactCtrl.activateUser(systemUsers.get(selectedIdx));
            NotificationManager.success("User Enabled", "User enabled successfully.");
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    private void clearForm() {
        newUsername.clear();
        newDisplayName.clear();
        newEmail.clear();
        newPassword.clear();
    }

    @FXML
    private void onZoom() {
        TableZoomUtil.openZoom(usersTable, "Users");
    }

    public static class UserRow {
        public final SimpleStringProperty username;
        public final SimpleStringProperty displayName;
        public final SimpleStringProperty email;
        public final SimpleStringProperty roles;
        public final SimpleStringProperty status;

        public UserRow(final String u, final String d, final String e,
                       final String r, final String s) {
            username = new SimpleStringProperty(u);
            displayName = new SimpleStringProperty(d);
            email = new SimpleStringProperty(e);
            roles = new SimpleStringProperty(r);
            status = new SimpleStringProperty(s);
        }
    }
}
