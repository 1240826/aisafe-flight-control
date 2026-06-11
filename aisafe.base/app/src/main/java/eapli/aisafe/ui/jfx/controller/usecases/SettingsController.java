package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.flightplan.application.TestFlightPlanController;
import eapli.aisafe.ui.jfx.util.FieldValidator;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.prefs.Preferences;

public class SettingsController {

    private static final String PREFS_HOST = "simulator.host";
    private static final String PREFS_PORT = "simulator.port";
    private static final String PREFS_TIMEOUT = "simulator.timeout";

    @FXML
    private Label javaVersion;

    @FXML
    private Label osInfo;

    @FXML
    private TextField simulatorHostField;

    @FXML
    private TextField simulatorPortField;

    @FXML
    private TextField simulatorTimeoutField;

    @FXML
    private Button saveSimulatorBtn;

    @FXML
    private Label connectionStatus;

    @FXML
    private Label simulatorHostFieldError;

    @FXML
    private Label simulatorPortFieldError;

    @FXML
    private Label simulatorTimeoutFieldError;

    @FXML
    private void initialize() {
        javaVersion.setText(System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        osInfo.setText(System.getProperty("os.name") + " " + System.getProperty("os.version"));

        final var prefs = Preferences.userNodeForPackage(SettingsController.class);
        final String defaultHost = System.getProperty("aisafe.simulator.host", "localhost");
        final String defaultPort = System.getProperty("aisafe.simulator.port", "9999");
        final String defaultTimeout = System.getProperty("aisafe.simulator.timeout", "180");
        final String savedHost = prefs.get(PREFS_HOST, defaultHost);
        final String savedPort = prefs.get(PREFS_PORT, defaultPort);
        final String savedTimeout = prefs.get(PREFS_TIMEOUT, defaultTimeout);
        simulatorHostField.setText(savedHost);
        simulatorPortField.setText(savedPort);
        simulatorTimeoutField.setText(savedTimeout);

        FieldValidator.onRequired(simulatorHostField, simulatorHostFieldError, "Host");
        FieldValidator.onPattern(simulatorHostField, simulatorHostFieldError,
                "^[a-zA-Z0-9.-]+$", "Host must be a valid hostname or IP.");
        FieldValidator.onRequiredInteger(simulatorPortField, simulatorPortFieldError, "Port");
        FieldValidator.onRequiredInteger(simulatorTimeoutField, simulatorTimeoutFieldError, "Timeout");
    }

    @FXML
    private void onSaveSimulatorConfig() {
        if (!FieldValidator.isFormValid(simulatorHostFieldError, simulatorPortFieldError, simulatorTimeoutFieldError)) {
            NotificationManager.error("Validation Error", "Fix the highlighted fields before submitting.");
            return;
        }
        final String host = simulatorHostField.getText().trim();
        final String portStr = simulatorPortField.getText().trim();
        final String timeoutStr = simulatorTimeoutField.getText().trim();

        int port = Integer.parseInt(portStr);
        if (port <= 0 || port > 65535) {
            NotificationManager.error("Invalid Port", "Port must be between 1 and 65535.");
            return;
        }

        int timeout = Integer.parseInt(timeoutStr);
        if (timeout < 10 || timeout > 3600) {
            NotificationManager.error("Invalid Timeout", "Timeout must be between 10 and 3600 seconds.");
            return;
        }

        final var prefs = Preferences.userNodeForPackage(SettingsController.class);
        prefs.put(PREFS_HOST, host);
        prefs.put(PREFS_PORT, portStr);
        prefs.put(PREFS_TIMEOUT, timeoutStr);

        // Also save to TestFlightPlanController's preference node for compatibility
        final var tprefs = Preferences.userNodeForPackage(TestFlightPlanController.class);
        tprefs.put(PREFS_HOST, host);
        tprefs.put(PREFS_PORT, portStr);

        // Set system properties so runner picks them up immediately
        System.setProperty("aisafe.simulator.host", host);
        System.setProperty("aisafe.simulator.port", portStr);
        System.setProperty("aisafe.simulator.timeout", timeoutStr);

        testConnection(host, port);
    }

    private void testConnection(final String host, final int port) {
        connectionStatus.setText("Testing connection to " + host + ":" + port + "...");
        connectionStatus.setStyle("-fx-text-fill: #d29922;");
        saveSimulatorBtn.setDisable(true);

        new Thread(() -> {
            try (final Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 5000);
                Platform.runLater(() -> {
                    connectionStatus.setText("Connected to " + host + ":" + port);
                    connectionStatus.setStyle("-fx-text-fill: #3fb950;");
                    NotificationManager.success("Connection OK", "Simulator reachable at " + host + ":" + port);
                });
            } catch (final IOException e) {
                Platform.runLater(() -> {
                    connectionStatus.setText("Cannot reach " + host + ":" + port + " (" + e.getMessage() + ")");
                    connectionStatus.setStyle("-fx-text-fill: #f85149;");
                    NotificationManager.error("Connection Failed",
                            "Could not connect to " + host + ":" + port
                            + "\nMake sure sim_server is running on the target machine.");
                });
            } finally {
                Platform.runLater(() -> saveSimulatorBtn.setDisable(false));
            }
        }).start();
    }
}
