package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.ui.jfx.controller.SessionManager;
import eapli.aisafe.ui.jfx.util.ConfirmationDialog;
import eapli.aisafe.ui.jfx.util.FieldValidator;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

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

    @FXML
    private Label newUsernameError;

    @FXML
    private Label newDisplayNameError;

    @FXML
    private Label newEmailError;

    @FXML
    private Label newPasswordError;

    @FXML
    private Label newRoleError;

    private final AddUserController addCtrl = new AddUserController();
    private final ListUsersController listCtrl = new ListUsersController();
    private final DeactivateUserController deactCtrl = new DeactivateUserController();

    private final ObservableList<UserRow> users = FXCollections.observableArrayList();
    private final List<SystemUser> systemUsers = new ArrayList<>();

    @FXML
    private void initialize() {
        colUsername.setCellValueFactory(d -> d.getValue().username);
        colDisplayName.setCellValueFactory(d -> d.getValue().displayName);
        colEmail.setCellValueFactory(d -> d.getValue().email);
        colRoles.setCellValueFactory(d -> d.getValue().roles);
        colStatus.setCellValueFactory(d -> d.getValue().status);

        FieldValidator.onRequired(newUsername, newUsernameError, "Username");
        FieldValidator.onRequired(newDisplayName, newDisplayNameError, "Display name");
        FieldValidator.onRequired(newEmail, newEmailError, "Email");
        FieldValidator.onPattern(newEmail, newEmailError, ".+@.+\\..+", "Email must be valid (e.g. user@domain.com).");
        FieldValidator.onRequired(newPassword, newPasswordError, "Password");
        FieldValidator.onRequiredCombo(newRole, newRoleError, "Role");

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

        filterRole.valueProperty().addListener((o, a, b) -> applyFilters());
        filterStatus.valueProperty().addListener((o, a, b) -> applyFilters());
        searchField.textProperty().addListener((o, a, b) -> applyFilters());

        loadData();
    }

    private void loadData() {
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
            applyFilters();
        } catch (final Exception e) {
            NotificationManager.error("Error", "Could not load users: " + e.getMessage());
        }
    }

    @FXML
    private void refreshTable() {
        loadData();
    }

    private void applyFilters() {
        final String roleFilter = filterRole.getValue();
        final String statusFilter = filterStatus.getValue();
        final String searchText = searchField.getText();

        final ObservableList<UserRow> filtered = FXCollections.observableArrayList();
        for (int i = 0; i < users.size(); i++) {
            final var u = systemUsers.get(i);
            final var row = users.get(i);

            if (roleFilter != null && !"All".equals(roleFilter)
                    && u.roleTypes().stream().noneMatch(r -> r.toString().equals(roleFilter))) {
                continue;
            }
            if (statusFilter != null && !"All".equals(statusFilter)
                    && !row.status.get().equalsIgnoreCase(statusFilter)) {
                continue;
            }
            if (searchText != null && !searchText.isBlank()
                    && !row.username.get().toLowerCase().contains(searchText.toLowerCase())
                    && !row.displayName.get().toLowerCase().contains(searchText.toLowerCase())
                    && !row.email.get().toLowerCase().contains(searchText.toLowerCase())
                    && !row.roles.get().toLowerCase().contains(searchText.toLowerCase())) {
                continue;
            }
            filtered.add(row);
        }
        usersTable.setItems(filtered);
    }

    @FXML
    private void addUser() {
        if (!FieldValidator.isFormValid(newUsernameError, newDisplayNameError,
                newEmailError, newPasswordError, newRoleError)) {
            NotificationManager.error("Validation Error", "Fix the highlighted fields before submitting.");
            return;
        }
        try {
            final var username = newUsername.getText();
            final var displayName = newDisplayName.getText();
            final var email = newEmail.getText();
            final var password = newPassword.getText();
            final var roleStr = newRole.getValue();

            addCtrl.addUser(username, password, displayName, "", email,
                    java.util.Set.of(Role.valueOf(roleStr)),
                    java.time.LocalDate.now().plusYears(1));

            NotificationManager.success("User Created", "User created successfully!");
            clearForm();
            loadData();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void disableUser() {
        final var selectedIdx = usersTable.getSelectionModel().getSelectedIndex();
        if (selectedIdx < 0) {
            NotificationManager.error("Selection Error", "Select a user to disable.");
            return;
        }
        final var realUser = findSystemUser(selectedIdx);
        if (realUser == null) return;

        if (!ConfirmationDialog.confirm("Disable User",
                "Are you sure you want to disable user " + realUser.username() + "?")) {
            return;
        }
        try {
            deactCtrl.deactivateUser(realUser);
            NotificationManager.success("User Disabled", "User disabled successfully.");
            loadData();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void enableUser() {
        final var selectedIdx = usersTable.getSelectionModel().getSelectedIndex();
        if (selectedIdx < 0) {
            NotificationManager.error("Selection Error", "Select a user to enable.");
            return;
        }
        final var realUser = findSystemUser(selectedIdx);
        if (realUser == null) return;

        if (!ConfirmationDialog.confirm("Enable User",
                "Are you sure you want to enable user " + realUser.username() + "?")) {
            return;
        }
        try {
            deactCtrl.activateUser(realUser);
            NotificationManager.success("User Enabled", "User enabled successfully.");
            loadData();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    private SystemUser findSystemUser(final int filteredIndex) {
        final var roleFilter = filterRole.getValue();
        final var statusFilter = filterStatus.getValue();
        final var searchText = searchField.getText();
        int realIdx = -1;
        int found = 0;
        for (int i = 0; i < users.size(); i++) {
            final var u = systemUsers.get(i);
            final var row = users.get(i);

            if (roleFilter != null && !"All".equals(roleFilter)
                    && u.roleTypes().stream().noneMatch(r -> r.toString().equals(roleFilter))) continue;
            if (statusFilter != null && !"All".equals(statusFilter)
                    && !row.status.get().equalsIgnoreCase(statusFilter)) continue;
            if (searchText != null && !searchText.isBlank()
                    && !row.username.get().toLowerCase().contains(searchText.toLowerCase())
                    && !row.displayName.get().toLowerCase().contains(searchText.toLowerCase())
                    && !row.email.get().toLowerCase().contains(searchText.toLowerCase())
                    && !row.roles.get().toLowerCase().contains(searchText.toLowerCase())) continue;

            if (found == filteredIndex) { realIdx = i; break; }
            found++;
        }
        if (realIdx >= 0 && realIdx < systemUsers.size()) return systemUsers.get(realIdx);
        return null;
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
