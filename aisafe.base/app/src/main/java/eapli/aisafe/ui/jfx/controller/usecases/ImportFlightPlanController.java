package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.flightplan.application.TestFlightPlanController;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ImportFlightPlanController {

    @FXML
    private TextField filePathField;

    @FXML
    private TextArea dslContent;

    @FXML
    private VBox validationResult;

    @FXML
    private Label validationTitle;

    @FXML
    private TextArea validationDetails;

    @FXML
    private Button simulateButton;

    @FXML
    private VBox simulationResult;

    @FXML
    private Label simulationTitle;

    @FXML
    private TextArea simulationReport;

    @FXML
    private ComboBox<String> exampleSelector;

    private final eapli.aisafe.flightplan.application.ImportFlightPlanController ctrl
            = new eapli.aisafe.flightplan.application.ImportFlightPlanController();

    private final TestFlightPlanController testCtrl = new TestFlightPlanController();

    private Path selectedFile;
    private String currentContent;
    private String currentFileName;
    private String lastImportedDesignator;

    private static final class ExampleEntry {
        final String label;
        final String resource;

        ExampleEntry(final String label, final String resource) {
            this.label = label;
            this.resource = resource;
        }
    }

    private static final List<ExampleEntry> EXAMPLES = List.of(
            new ExampleEntry("✅ Valid: LIS → OPO (charter)", "/examples/valid_lis_opo.flightplan"),
            new ExampleEntry("✅ Valid: LIS → LHR (regular)", "/examples/valid_direct_flight.flightplan"),
            new ExampleEntry("✅ Valid: LIS → MAD (charter)", "/examples/valid_demo_charter_lis_mad.flightplan"),
            new ExampleEntry("✅ Valid: Short hop", "/examples/valid_short_hop.flightplan"),
            new ExampleEntry("✅ Valid: Multi-leg", "/examples/valid_multi_leg.flightplan"),
            new ExampleEntry("✅ Valid: Long haul charter", "/examples/valid_long_haul_charter.flightplan"),
            new ExampleEntry("✅ Valid: OPO → WAW", "/examples/valid_opo_waw.flightplan"),
            new ExampleEntry("❌ Invalid: Bad airport", "/examples/invalid_bad_airport.flightplan"),
            new ExampleEntry("❌ Invalid: Missing fuel", "/examples/invalid_missing_fuel.flightplan"),
            new ExampleEntry("❌ Invalid: Bad time format", "/examples/invalid_bad_time_format.flightplan"),
            new ExampleEntry("❌ Invalid: Duplicate ID", "/examples/invalid_sem_duplicate_id.flightplan")
    );

    @FXML
    private void initialize() {
        for (final var ex : EXAMPLES) {
            exampleSelector.getItems().add(ex.label);
        }
        exampleSelector.getSelectionModel().selectFirst();
    }

    @FXML
    private void loadExample() {
        final int idx = exampleSelector.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= EXAMPLES.size()) return;

        final var entry = EXAMPLES.get(idx);
        try (final var is = getClass().getResourceAsStream(entry.resource)) {
            if (is == null) {
                NotificationManager.error("Error", "Example file not found: " + entry.resource);
                return;
            }
            currentContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            currentFileName = entry.resource.substring(entry.resource.lastIndexOf('/') + 1);
            dslContent.setText(currentContent);
            filePathField.setText("Example: " + currentFileName);
            selectedFile = null;
            validationResult.setVisible(false);
            validationResult.setManaged(false);
            simulationResult.setVisible(false);
            simulationResult.setManaged(false);
        } catch (final IOException e) {
            NotificationManager.error("Error", "Could not load example: " + e.getMessage());
        }
    }

    @FXML
    private void chooseFile() {
        final var chooser = new FileChooser();
        chooser.setTitle("Select Flight Plan File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Flight Plan Files", "*.txt", "*.flight", "*.dsl"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        final var file = chooser.showOpenDialog(null);
        if (file != null) {
            selectedFile = file.toPath();
            currentFileName = file.getName();
            filePathField.setText(file.getAbsolutePath());
            try {
                currentContent = Files.readString(selectedFile);
                dslContent.setText(currentContent);
                validationResult.setVisible(false);
                validationResult.setManaged(false);
                simulationResult.setVisible(false);
                simulationResult.setManaged(false);
            } catch (final IOException e) {
                NotificationManager.error("File Error", "Could not read file: " + e.getMessage());
            }
        }
    }

    @FXML
    private void importPlan() {
        if (currentContent == null || currentContent.isBlank()) {
            NotificationManager.error("No Content", "Load or select a flight plan first.");
            return;
        }
        try {
            final var result = ctrl.importFlightPlan(currentContent,
                    currentFileName != null ? currentFileName : "unknown", "AUTO");
            validationResult.setVisible(true);
            validationResult.setManaged(true);
            if (result != null && result.allPassed()) {
                validationTitle.setText("✅ Validation & Import Passed");
                validationDetails.setStyle("-fx-text-fill: #3fb950;");
                validationDetails.setText("All checks passed. Flight plan imported successfully.");
                // Extract designator for simulation
                try {
                    lastImportedDesignator = ctrl.extractFlightDesignator(currentContent);
                } catch (final Exception ignored) {
                    lastImportedDesignator = null;
                }
                simulateButton.setVisible(true);
                simulateButton.setManaged(true);
                simulationResult.setVisible(false);
                simulationResult.setManaged(false);
                NotificationManager.success("Import Successful",
                        "Flight plan imported. Click ▶ Run Simulation to test it.");
            } else {
                validationTitle.setText("❌ Validation Failed");
                validationDetails.setStyle("-fx-text-fill: #f85149;");
                final var sb = new StringBuilder();
                if (result != null) {
                    result.allErrors().forEach(e -> sb.append("• ").append(e).append("\n"));
                } else {
                    sb.append("Unknown validation error.");
                }
                validationDetails.setText(sb.toString());
                simulateButton.setVisible(false);
                simulateButton.setManaged(false);
                NotificationManager.error("Import Failed", "Validation errors found. See details below.");
            }
        } catch (final Exception e) {
            NotificationManager.error("Import Failed", e.getMessage());
        }
    }

    @FXML
    private void runSimulation() {
        if (lastImportedDesignator == null) {
            NotificationManager.error("No Flight", "Import a flight plan first.");
            return;
        }
        simulateButton.setText("⏳ Running simulation...");
        simulateButton.setDisable(true);
        simulationResult.setVisible(false);
        simulationResult.setManaged(false);

        Platform.runLater(() -> {
            try {
                final var result = testCtrl.testFlightPlan(lastImportedDesignator, "AUTO");
                simulationResult.setVisible(true);
                simulationResult.setManaged(true);
                if (result != null && result.passed()) {
                    simulationTitle.setText("✅ Simulation PASSED");
                    simulationTitle.setStyle("-fx-text-fill: #3fb950;");
                } else {
                    simulationTitle.setText("❌ Simulation FAILED");
                    simulationTitle.setStyle("-fx-text-fill: #f85149;");
                }
                final var report = result != null ? result.reportContent() : "No report available.";
                simulationReport.setText(report != null ? report : "No report content.");
                simulationReport.setStyle("-fx-text-fill: #e6edf3;");
                final var msg = result != null ? result.message() : "Unknown error";
                NotificationManager.info("Simulation Complete", msg);
            } catch (final Exception e) {
                simulationResult.setVisible(true);
                simulationResult.setManaged(true);
                simulationTitle.setText("❌ Simulation Error");
                simulationTitle.setStyle("-fx-text-fill: #f85149;");
                simulationReport.setText("Error: " + e.getMessage());
                NotificationManager.error("Simulation Failed", e.getMessage());
            } finally {
                simulateButton.setText("▶ Run Simulation");
                simulateButton.setDisable(false);
            }
        });
    }

    @FXML
    private void addWeatherData() {
        final var mainController = (eapli.aisafe.ui.jfx.controller.MainController)
                dslContent.getScene().getUserData();
        if (mainController != null) {
            mainController.navigateFromChild("Weather Data",
                    "/fxml/usecases/WeatherData.fxml", new WeatherDataController());
        } else {
            NotificationManager.info("Navigate", "Go to Weather Data to register observations.");
        }
    }
}
