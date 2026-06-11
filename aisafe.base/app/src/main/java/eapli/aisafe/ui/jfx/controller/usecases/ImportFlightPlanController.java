package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.flight.application.AddWeatherToFlightController;
import eapli.aisafe.ui.jfx.controller.MainController;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    private ComboBox<String> exampleSelector;

    @FXML
    private VBox weatherSection;

    @FXML
    private ComboBox<WeatherDataItem> weatherSelector;

    @FXML
    private Button assignWeatherBtn;

    @FXML
    private Button addWeatherNavBtn;

    @FXML
    private Label weatherStatus;

    private final eapli.aisafe.flightplan.application.ImportFlightPlanController ctrl
            = new eapli.aisafe.flightplan.application.ImportFlightPlanController();

    private final AddWeatherToFlightController weatherCtrl = new AddWeatherToFlightController();

    private Path selectedFile;
    private String currentContent;
    private String currentFileName;
    private String lastImportedDesignator;
    private WeatherDataItem assignedWeather;

    private static final class WeatherDataItem {
        final Long id;
        final String display;

        WeatherDataItem(final Long id, final String display) {
            this.id = id;
            this.display = display;
        }

        @Override
        public String toString() { return display; }
    }

    private static final class ExampleEntry {
        final String label;
        final String resource;

        ExampleEntry(final String label, final String resource) {
            this.label = label;
            this.resource = resource;
        }
    }

    private static final List<ExampleEntry> EXAMPLES = List.of(
            new ExampleEntry("Valid: LIS \u2192 OPO (charter)", "/examples/valid_lis_opo.flightplan"),
            new ExampleEntry("Valid: LIS \u2192 LHR (regular)", "/examples/valid_direct_flight.flightplan"),
            new ExampleEntry("Valid: LIS \u2192 MAD (charter)", "/examples/valid_demo_charter_lis_mad.flightplan"),
            new ExampleEntry("Valid: Short hop", "/examples/valid_short_hop.flightplan"),
            new ExampleEntry("Valid: Multi-leg", "/examples/valid_multi_leg.flightplan"),
            new ExampleEntry("Valid: Long haul charter", "/examples/valid_long_haul_charter.flightplan"),
            new ExampleEntry("Valid: OPO \u2192 WAW", "/examples/valid_opo_waw.flightplan"),
            new ExampleEntry("Conflict: LIS \u2192 OPO (Flight A)", "/examples/valid_demo_conflict_a.flightplan"),
            new ExampleEntry("Conflict: LIS \u2192 OPO (Flight B)", "/examples/valid_demo_conflict_b.flightplan"),
            new ExampleEntry("Conflict: 3-way LIS \u2192 OPO", "/examples/valid_conflict_3way.flightplan"),
            new ExampleEntry("Conflict: Crossing LIS \u2192 MAD", "/examples/valid_conflict_crossing.flightplan"),
            new ExampleEntry("Conflict: Crossing OPO \u2192 CDG", "/examples/valid_conflict_crossing2.flightplan"),
            new ExampleEntry("Conflict: Overtake fast LIS \u2192 OPO", "/examples/valid_conflict_overtake.flightplan"),
            new ExampleEntry("Conflict: Overtake slow LIS \u2192 OPO", "/examples/valid_conflict_overtake2.flightplan"),
            new ExampleEntry("Conflict: Multi-aircraft LIS \u2192 FNC", "/examples/valid_conflict_multiaircraft.flightplan"),
            new ExampleEntry("Conflict: Multi-aircraft LIS \u2192 PDL", "/examples/valid_conflict_multiaircraft2.flightplan"),
            new ExampleEntry("Conflict: Multi-aircraft LIS \u2192 RAI", "/examples/valid_conflict_multiaircraft3.flightplan"),
            new ExampleEntry("Conflict: Multi-aircraft LIS \u2192 SID", "/examples/valid_conflict_multiaircraft4.flightplan"),
            new ExampleEntry("Invalid: Bad airport", "/examples/invalid_bad_airport.flightplan"),
            new ExampleEntry("Invalid: Missing fuel", "/examples/invalid_missing_fuel.flightplan"),
            new ExampleEntry("Invalid: Bad time format", "/examples/invalid_bad_time_format.flightplan"),
            new ExampleEntry("Invalid: Duplicate ID", "/examples/invalid_sem_duplicate_id.flightplan")
    );

    @FXML
    private void initialize() {
        for (final var ex : EXAMPLES) {
            exampleSelector.getItems().add(ex.label);
        }
        exampleSelector.getSelectionModel().selectFirst();

        weatherSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(final WeatherDataItem item, final boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.display);
                setStyle(empty ? "-fx-background-color: transparent;" : "-fx-text-fill: #e6edf3;");
            }
        });
        weatherSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(final WeatherDataItem item, final boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.display);
            }
        });
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
            weatherSection.setVisible(false);
            weatherSection.setManaged(false);
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
                weatherSection.setVisible(false);
                weatherSection.setManaged(false);
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
                validationTitle.setText("\u2713 Validation & Import Passed");
                validationDetails.setStyle("-fx-text-fill: #3fb950;");
                final var sb = new StringBuilder("All checks passed.\n");
                if (result.summary() != null) sb.append(result.summary());
                validationDetails.setText(sb.toString());
                try {
                    lastImportedDesignator = ctrl.extractFlightDesignator(currentContent);
                } catch (final Exception ignored) {
                    lastImportedDesignator = null;
                }
                loadWeatherForFlight();
                NotificationManager.success("Import Successful", "Flight plan imported.");
            } else {
                validationTitle.setText("\u2715 Validation Failed");
                validationDetails.setStyle("-fx-text-fill: #f85149;");
                final var sb = new StringBuilder("Validation errors:\n");
                if (result != null) {
                    result.allErrors().forEach(e -> sb.append("\u2022 ").append(e).append("\n"));
                } else {
                    sb.append("Unknown validation error.");
                }
                validationDetails.setText(sb.toString());
                NotificationManager.error("Import Failed", "Validation errors found.");
            }
        } catch (final Exception e) {
            NotificationManager.error("Import Failed", e.getMessage());
        }
    }

    private void loadWeatherForFlight() {
        if (lastImportedDesignator == null) return;
        try {
            final var flight = weatherCtrl.flightByDesignator(lastImportedDesignator);
            if (flight == null) return;

            final var wdList = weatherCtrl.weatherDataForFlight(flight);
            final var items = FXCollections.<WeatherDataItem>observableArrayList();
            for (final var wd : wdList) {
                final var area = wd.areaCode() != null ? wd.areaCode().toString() : "N/A";
                items.add(new WeatherDataItem(wd.identity(),
                        area + " | " + wd.recordedDateTime() + " | " + wd.temperatureCelsius() + "\u00b0C | "
                        + wd.windCondition().speedKnots() + "kt " + wd.windCondition().directionDegrees() + "\u00b0"));
            }

            final Long existingId = flight.weatherDataId();
            if (existingId != null) {
                for (final var item : items) {
                    if (item.id.equals(existingId)) {
                        assignedWeather = item;
                        weatherStatus.setText("Assigned: " + item.display);
                        weatherStatus.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 12px;");
                        break;
                    }
                }
            }

            if (!items.isEmpty()) {
                weatherSelector.setItems(items);
                weatherSelector.getSelectionModel().select(assignedWeather);
                weatherSection.setVisible(true);
                weatherSection.setManaged(true);
            }
        } catch (final Exception ignored) {
        }
    }

    @FXML
    private void onAssignWeather() {
        final var selected = weatherSelector.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationManager.error("No Selection", "Select weather data first.");
            return;
        }
        if (lastImportedDesignator == null) {
            NotificationManager.error("No Flight", "Import a flight plan first.");
            return;
        }
        try {
            weatherCtrl.assignWeather(lastImportedDesignator, selected.id);
            assignedWeather = selected;
            weatherStatus.setText("Assigned: " + selected.display);
            weatherStatus.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 12px;");
            NotificationManager.success("Weather Assigned", "Weather data linked to flight " + lastImportedDesignator);
        } catch (final Exception e) {
            NotificationManager.error("Assignment Failed", e.getMessage());
        }
    }

    @FXML
    private void onAddNewWeather() {
        final var scene = dslContent.getScene();
        if (scene != null) {
            final var mainController = (MainController) scene.getUserData();
            if (mainController != null) {
                mainController.navigateFromChild("Weather Data",
                        "/fxml/usecases/WeatherData.fxml", new WeatherDataController());
            }
        }
    }

    @FXML
    private void onZoomDsl() {
        final var text = dslContent.getText();
        if (text == null || text.isBlank()) return;
        showZoomWindow("Flight Plan DSL - " + (currentFileName != null ? currentFileName : ""),
                text, false, 900, 600);
    }

    @FXML
    private void onZoomValidation() {
        final var text = validationDetails.getText();
        if (text == null || text.isBlank()) return;
        showZoomWindow("Validation Details", text, true, 700, 500);
    }

    private void showZoomWindow(final String title, final String content,
                                 final boolean wrapText, final int w, final int h) {
        final var stage = new Stage();
        stage.setTitle(title);
        final var area = new TextArea(content);
        area.setEditable(false);
        area.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #e6edf3;"
                + "-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px;");
        area.setWrapText(wrapText);
        final VBox root = new VBox(area);
        VBox.setVgrow(area, javafx.scene.layout.Priority.ALWAYS);
        final var scene = new Scene(root, w, h);
        final var url = getClass().getResource("/styles/dark-theme.css");
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
}
