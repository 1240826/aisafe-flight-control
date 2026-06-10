package eapli.aisafe.ui.jfx.controller.usecases;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SettingsController {

    @FXML
    private Label javaVersion;

    @FXML
    private Label osInfo;

    @FXML
    private Label simulatorHost;

    @FXML
    private Label simulatorPort;

    @FXML
    private void initialize() {
        javaVersion.setText(System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        osInfo.setText(System.getProperty("os.name") + " " + System.getProperty("os.version"));
        simulatorHost.setText(System.getProperty("aisafe.simulator.host", "localhost"));
        simulatorPort.setText(System.getProperty("aisafe.simulator.port", "8080"));
    }
}
