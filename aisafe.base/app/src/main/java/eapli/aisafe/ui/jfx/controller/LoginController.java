package eapli.aisafe.ui.jfx.controller;

import eapli.aisafe.ui.jfx.SceneManager;
import eapli.framework.infrastructure.authz.application.Authenticator;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private VBox loginForm;

    private final Authenticator authenticator = AuthzRegistry.authenticationService();

    @FXML
    private void initialize() {
        Platform.runLater(() -> usernameField.requestFocus());
        loginForm.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ENTER && loginForm.getScene() != null && loginForm.isVisible()) {
                        handleLogin();
                    }
                });
            }
        });
    }

    @FXML
    private void handleLogin() {
        final String username = usernameField.getText();
        final String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            showError("Please enter your username and password.");
            return;
        }

        try {
            final var user = authenticator.authenticate(username, password);
            if (user.isPresent()) {
                SessionManager.login(user.get());
                SceneManager.switchScene("AISafe Flight Control System", "/fxml/Main.fxml",
                        () -> new MainController());
            } else {
                showError("Invalid credentials.");
                passwordField.clear();
                passwordField.requestFocus();
            }
        } catch (final Exception e) {
            System.err.println("[Login] Error during login:");
            e.printStackTrace(System.err);
            showError("System error. Please try again.");
            passwordField.clear();
        }
    }

    @FXML
    private void handleCreateAccount() {
        errorLabel.setText("Contact your system administrator.");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showError(final String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
