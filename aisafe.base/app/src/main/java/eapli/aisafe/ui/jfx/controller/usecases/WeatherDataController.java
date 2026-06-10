package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.weatherdata.application.RegisterWeatherDataController;
import eapli.aisafe.weatherdata.application.ImportBulkWeatherDataController;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.StreamSupport;

public class WeatherDataController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterArea;

    @FXML
    private DatePicker filterDate;

    @FXML
    private TableView<WeatherRow> weatherTable;

    @FXML
    private TableColumn<WeatherRow, String> colDate;

    @FXML
    private TableColumn<WeatherRow, String> colArea;

    @FXML
    private TableColumn<WeatherRow, String> colWindSpeed;

    @FXML
    private TableColumn<WeatherRow, String> colWindDir;

    @FXML
    private TableColumn<WeatherRow, String> colTemp;

    @FXML
    private TableColumn<WeatherRow, String> colProvider;

    @FXML
    private DatePicker newDate;

    @FXML
    private ComboBox<String> newArea;

    @FXML
    private TextField newWindSpeed;

    @FXML
    private TextField newWindDir;

    @FXML
    private TextField newLat;

    @FXML
    private TextField newLon;

    @FXML
    private TextField newAlt;

    @FXML
    private TextField newTemp;

    @FXML
    private TextField newProvider;

    @FXML
    private TextField csvPath;

    private final RegisterWeatherDataController regCtrl = new RegisterWeatherDataController();
    private final ImportBulkWeatherDataController bulkCtrl = new ImportBulkWeatherDataController();

    private final ObservableList<WeatherRow> items = FXCollections.observableArrayList();
    private String selectedAreaCode;

    @FXML
    private void initialize() {
        colDate.setCellValueFactory(d -> d.getValue().date);
        colArea.setCellValueFactory(d -> d.getValue().area);
        colWindSpeed.setCellValueFactory(d -> d.getValue().windSpeed);
        colWindDir.setCellValueFactory(d -> d.getValue().windDir);
        colTemp.setCellValueFactory(d -> d.getValue().temp);
        colProvider.setCellValueFactory(d -> d.getValue().provider);

        loadAreas();
    }

    private void loadAreas() {
        try {
            final var areas = new eapli.aisafe.aircontrolarea.application
                    .RegisterAirControlAreaController().allAirControlAreas();
            StreamSupport.stream(areas.spliterator(), false)
                    .forEach(a -> {
                        newArea.getItems().add(a.identity().toString());
                        filterArea.getItems().add(a.identity().toString());
                    });
        } catch (final Exception e) {
            NotificationManager.error("Error", "Could not load areas: " + e.getMessage());
        }
        if (!newArea.getItems().isEmpty()) {
            newArea.getSelectionModel().selectFirst();
        }
        filterArea.getItems().add(0, "All");
        filterArea.getSelectionModel().selectFirst();
    }

    @FXML
    private void refreshTable() {
        items.clear();
        final String areaCode = newArea.getValue();
        if (areaCode == null) return;
        try {
            final var records = regCtrl.weatherDataForArea(areaCode);
            StreamSupport.stream(records.spliterator(), false)
                    .forEach(w -> {
                        final var wind = w.windCondition();
                        items.add(new WeatherRow(
                                w.recordedDateTime().toString(),
                                w.areaCode().toString(),
                                String.valueOf(wind.speedKnots()),
                                wind.directionDegrees() + "\u00B0",
                                String.format("%.1f", w.temperatureCelsius()),
                                w.sourceProvider()
                        ));
                    });
            weatherTable.setItems(items);
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void addWeather() {
        try {
            final var date = newDate.getValue();
            final var area = newArea.getValue();

            if (date == null || area == null) {
                NotificationManager.error("Validation Error", "Date and area are required.");
                return;
            }

            final double lat = parseDouble(newLat, 0.0);
            final double lon = parseDouble(newLon, 0.0);
            final int alt = (int) parseDouble(newAlt, 0);
            final double speed = parseDouble(newWindSpeed, 1.0);
            final double dir = parseDouble(newWindDir, 0.0);
            final double temp = parseDouble(newTemp, 15.0);
            final String provider = newProvider.getText().isBlank() ? "MANUAL" : newProvider.getText();

            regCtrl.registerWeatherData(
                    area, lat, lon, alt, speed, dir, temp, provider,
                    LocalDateTime.of(date, LocalTime.now()));
            NotificationManager.success("Weather Data", "Weather data registered!");
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    private double parseDouble(final TextField field, final double fallback) {
        final var val = field.getText();
        if (val == null || val.isBlank()) return fallback;
        try { return Double.parseDouble(val); } catch (final NumberFormatException e) { return fallback; }
    }

    @FXML
    private void chooseCsv() {
        final var chooser = new FileChooser();
        chooser.setTitle("Select Weather Data CSV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        final var file = chooser.showOpenDialog(null);
        if (file != null) {
            csvPath.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void importCsv() {
        if (csvPath.getText() == null || csvPath.getText().isBlank()) {
            NotificationManager.error("No File", "Please select a CSV file first.");
            return;
        }
        try {
            final var path = Path.of(csvPath.getText());
            final var result = bulkCtrl.importFromCsv(path);
            final var msg = "Imported: " + result.imported() + " | Skipped: " + result.skipped()
                    + (result.hasErrors() ? " | Errors: " + String.join("; ", result.errors()) : "");
            if (result.hasErrors()) {
                NotificationManager.error("Import Complete", msg);
            } else {
                NotificationManager.success("Import Complete", msg);
            }
            refreshTable();
        } catch (final IOException e) {
            NotificationManager.error("Error", "Error reading file: " + e.getMessage());
        } catch (final Exception e) {
            NotificationManager.error("Import Error", e.getMessage());
        }
    }

    @FXML
    private void onZoom() {
        TableZoomUtil.openZoom(weatherTable, "Weather Data");
    }

    public static class WeatherRow {
        public final SimpleStringProperty date;
        public final SimpleStringProperty area;
        public final SimpleStringProperty windSpeed;
        public final SimpleStringProperty windDir;
        public final SimpleStringProperty temp;
        public final SimpleStringProperty provider;

        public WeatherRow(final String d, final String a, final String ws, final String wd,
                          final String t, final String p) {
            date = new SimpleStringProperty(d);
            area = new SimpleStringProperty(a);
            windSpeed = new SimpleStringProperty(ws);
            windDir = new SimpleStringProperty(wd);
            temp = new SimpleStringProperty(t);
            provider = new SimpleStringProperty(p);
        }
    }
}
