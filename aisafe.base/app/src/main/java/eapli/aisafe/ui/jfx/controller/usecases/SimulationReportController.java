package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.simulation.application.GenerateSimulationReportController;
import eapli.aisafe.simulation.domain.Simulation;
import eapli.aisafe.simulation.domain.ValidationResult;
import eapli.aisafe.ui.jfx.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

public class SimulationReportController {

    @FXML
    private ComboBox<String> simulationSelector;

    @FXML
    private Label liveIndicator;

    @FXML
    private Label metricFlights;

    @FXML
    private Label metricViolations;

    @FXML
    private Label metricPassRate;

    @FXML
    private Label metricCompanies;

    @FXML
    private Label simAreaLabel;

    @FXML
    private Label simTimeRangeLabel;

    @FXML
    private Label simSafetyThresholdLabel;

    @FXML
    private Label simStatusLabel;

    @FXML
    private VBox chartsContainer;

    @FXML
    private BarChart<String, Number> violationsChart;

    @FXML
    private CategoryAxis violationsXAxis;

    @FXML
    private NumberAxis violationsYAxis;

    @FXML
    private PieChart validationPieChart;

    @FXML
    private LineChart<Number, Number> violationsTrendChart;

    @FXML
    private NumberAxis trendXAxis;

    @FXML
    private NumberAxis trendYAxis;

    @FXML
    private BarChart<String, Number> companyChart;

    @FXML
    private CategoryAxis companyXAxis;

    @FXML
    private NumberAxis companyYAxis;

    @FXML
    private BarChart<String, Number> flightsPerHourChart;

    @FXML
    private CategoryAxis hourXAxis;

    @FXML
    private NumberAxis hourYAxis;

    @FXML
    private TextArea reportOutput;

    private final GenerateSimulationReportController ctrl = new GenerateSimulationReportController();
    private final FlightRepository flightRepo = PersistenceContext.repositories().flights();
    private final ObservableList<SimulationRow> sims = FXCollections.observableArrayList();
    private Timeline autoRefresh;
    private boolean liveMode = false;

    private static final Pattern SUMMARY_LINE = Pattern.compile(
            "Flights:\\s*(\\d+)\\s+Violations:\\s*(\\d+)");
    private static final Pattern FLIGHT_LINE = Pattern.compile(
            "[\\s●•]*([A-Z]{2}\\d{3,4})\\s+(\\w+)\\s+violations=(\\d+)");
    private static final Pattern RESULT_LINE = Pattern.compile(
            "RESULT:\\s*(PASS|FAIL)", Pattern.CASE_INSENSITIVE);

    @FXML
    private void initialize() {
        loadSimulations();
        simulationSelector.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) showSimulationDetails(sel);
        });
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (liveMode && simulationSelector.getValue() != null) {
                reloadSimulationData();
                refreshCharts();
            }
        }));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    @FXML
    private void loadSimulations() {
        try {
            sims.clear();
            StreamSupport.stream(ctrl.allSimulations().spliterator(), false)
                    .forEach(s -> sims.add(new SimulationRow(s)));
            simulationSelector.setItems(FXCollections.observableArrayList(
                    sims.stream().map(SimulationRow::getDisplayName).toList()));
            if (!sims.isEmpty()) {
                simulationSelector.getSelectionModel().selectFirst();
                showSimulationDetails(sims.get(0).getDisplayName());
            }
        } catch (final Exception e) {
            reportOutput.setText("Error loading simulations: " + e.getMessage());
        }
    }

    private void reloadSimulationData() {
        try {
            final var selectedName = simulationSelector.getValue();
            final var freshSims = new ArrayList<SimulationRow>();
            StreamSupport.stream(ctrl.allSimulations().spliterator(), false)
                    .forEach(s -> freshSims.add(new SimulationRow(s)));
            sims.clear();
            sims.addAll(freshSims);
            final var items = FXCollections.observableArrayList(
                    freshSims.stream().map(SimulationRow::getDisplayName).toList());
            simulationSelector.setItems(items);
            if (selectedName != null && items.contains(selectedName)) {
                simulationSelector.setValue(selectedName);
            } else if (!items.isEmpty()) {
                simulationSelector.getSelectionModel().selectFirst();
            }
        } catch (final Exception ignored) {}
    }

    private void showSimulationDetails(final String displayName) {
        final var sim = sims.stream()
                .filter(s -> s.getDisplayName().equals(displayName))
                .findFirst().orElse(null);
        if (sim == null) return;

        simAreaLabel.setText(sim.areaCode());
        simTimeRangeLabel.setText(sim.timeRange());
        simSafetyThresholdLabel.setText("Threshold: " + sim.thresholdValue() + " " + sim.thresholdUnit());

        final var result = sim.validationResult();
        simStatusLabel.setText(result.toString());
        simStatusLabel.getStyleClass().removeAll("status-active", "status-error", "status-warning");
        if (result == ValidationResult.PASSED) simStatusLabel.getStyleClass().add("status-active");
        else if (result == ValidationResult.FAILED) simStatusLabel.getStyleClass().add("status-error");
        else simStatusLabel.getStyleClass().add("status-warning");

        reportOutput.setText(sim.reportContent());
        liveMode = true;
        liveIndicator.setStyle("-fx-text-fill: #3fb950;");
        refreshCharts();
    }

    private void refreshCharts() {
        final var selected = simulationSelector.getValue();
        if (selected == null) return;
        final var sim = sims.stream()
                .filter(s -> s.getDisplayName().equals(selected))
                .findFirst().orElse(null);
        if (sim == null) return;

        final var reportText = sim.reportContent();

        final var parsed = parseCReport(reportText);
        final var flightIds = parsed.flightIds();
        final var perFlight = parsed.perFlightViolations();

        final var totalFlights = parsed.totalFlights();
        final var totalViolations = parsed.totalViolations();

        final var companies = lookupCompanies(flightIds);
        final var validationResult = parsed.validationResult() != null
                ? parsed.validationResult() : sim.validationResult();

        updateMetrics(totalFlights, totalViolations, companies.size(), validationResult);
        buildViolationsChart(perFlight);
        buildValidationPieChart(validationResult);
        buildViolationsTrendChart(parsed.trend());
        buildCompanyChart(companies, flightIds);
        buildFlightsPerHourChart(sim);
    }

    private static int sum(final Map<String, Integer> map) {
        return map.values().stream().mapToInt(Integer::intValue).sum();
    }

    private void updateMetrics(final int totalFlights, final int totalViolations,
                                final int totalCompanies, final ValidationResult result) {
        metricFlights.setText(String.valueOf(totalFlights));
        metricViolations.setText(String.valueOf(totalViolations));
        metricCompanies.setText(String.valueOf(totalCompanies));
        metricPassRate.setText(result == ValidationResult.PASSED ? "100%" :
                result == ValidationResult.FAILED ? "0%" : "Pending");
    }

    private CReportParseResult parseCReport(final String content) {
        int totalFlights = 0;
        int totalViolations = 0;
        ValidationResult vr = null;
        final List<String> flightIds = new ArrayList<>();
        final Map<String, Integer> perFlightViolations = new LinkedHashMap<>();
        final List<Integer> trend = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return new CReportParseResult(0, 0, null, List.of(), Map.of(), trend);
        }

        // Parse only from "SIMULATION SUMMARY" to end
        final var lines = content.lines().toList();
        int summaryStart = 0;
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).contains("SIMULATION SUMMARY")) {
                summaryStart = i;
                break;
            }
        }

        for (int i = summaryStart; i < lines.size(); i++) {
            final var line = lines.get(i);

            final var summaryMatcher = SUMMARY_LINE.matcher(line);
            if (summaryMatcher.find()) {
                totalFlights = Integer.parseInt(summaryMatcher.group(1));
                totalViolations = Integer.parseInt(summaryMatcher.group(2));
            }

            final var resultMatcher = RESULT_LINE.matcher(line);
            if (resultMatcher.find()) {
                vr = "PASS".equalsIgnoreCase(resultMatcher.group(1))
                        ? ValidationResult.PASSED : ValidationResult.FAILED;
            }

            final var flightMatcher = FLIGHT_LINE.matcher(line);
            if (flightMatcher.find()) {
                final var fid = flightMatcher.group(1);
                final var fv = Integer.parseInt(flightMatcher.group(3));
                flightIds.add(fid);
                perFlightViolations.put(fid, fv);
            }

            // Parse violation details for trend
            final var lower = line.toLowerCase();
            if (lower.contains("step=") && lower.contains("<->")) {
                trend.add(1);
            }
        }

        return new CReportParseResult(totalFlights, totalViolations, vr,
                flightIds, perFlightViolations, trend);
    }

    private Map<String, Integer> lookupCompanies(final List<String> flightIds) {
        final Map<String, Integer> result = new LinkedHashMap<>();
        for (final var fid : flightIds) {
            try {
                final var designator = FlightDesignator.valueOf(fid);
                final var flight = flightRepo.ofIdentity(designator).orElse(null);
                if (flight != null && flight.routeName() != null) {
                    final var routeName = flight.routeName().toString();
                    // Extract company from route name prefix (e.g., "TP123" -> "TP")
                    final var company = routeName.replaceAll("\\d+", "");
                    if (!company.isEmpty()) {
                        result.merge(company.toUpperCase(), 1, Integer::sum);
                    }
                }
            } catch (final Exception ignored) {}
        }
        if (result.isEmpty() && !flightIds.isEmpty()) {
            result.put("Unknown", flightIds.size());
        }
        return result;
    }

    private void buildViolationsChart(final Map<String, Integer> perFlight) {
        violationsChart.getData().clear();
        final var series = new XYChart.Series<String, Number>();
        series.setName("Violations");
        if (perFlight.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No Violations", 0));
        } else {
            perFlight.forEach((flight, count) ->
                    series.getData().add(new XYChart.Data<>(flight, count)));
        }
        violationsChart.getData().add(series);
    }

    private void buildValidationPieChart(final ValidationResult result) {
        validationPieChart.getData().clear();
        final var passed = new PieChart.Data("Passed", 0);
        final var failed = new PieChart.Data("Failed", 0);
        final var pending = new PieChart.Data("Pending", 0);

        switch (result) {
            case PASSED -> passed.pieValueProperty().set(1);
            case FAILED -> failed.pieValueProperty().set(1);
            default -> pending.pieValueProperty().set(1);
        }
        validationPieChart.getData().addAll(passed, failed, pending);

        validationPieChart.getData().forEach(d -> {
            if ("Passed".equals(d.getName())) d.getNode().setStyle("-fx-pie-color: #3fb950;");
            else if ("Failed".equals(d.getName())) d.getNode().setStyle("-fx-pie-color: #f85149;");
            else d.getNode().setStyle("-fx-pie-color: #d29922;");
        });
    }

    private void buildViolationsTrendChart(final List<Integer> trend) {
        violationsTrendChart.getData().clear();
        if (trend.isEmpty()) {
            final var series = new XYChart.Series<Number, Number>();
            series.setName("Violations");
            series.getData().add(new XYChart.Data<>(0, 0));
            violationsTrendChart.getData().add(series);
            return;
        }
        final var series = new XYChart.Series<Number, Number>();
        series.setName("Violations");
        for (int i = 0; i < trend.size(); i++) {
            series.getData().add(new XYChart.Data<>(i, trend.get(i)));
        }
        violationsTrendChart.getData().add(series);
    }

    private void buildCompanyChart(final Map<String, Integer> companies,
                                    final List<String> flightIds) {
        companyChart.getData().clear();
        final var series = new XYChart.Series<String, Number>();
        series.setName("Flights");
        if (companies.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Unknown", flightIds.size()));
        } else {
            companies.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
        }
        companyChart.getData().add(series);
    }

    private void buildFlightsPerHourChart(final SimulationRow sim) {
        flightsPerHourChart.getData().clear();
        final var series = new XYChart.Series<String, Number>();
        series.setName("Flights");
        // Parse time range from sim metadata
        try {
            final var range = sim.timeRange();
            final var parts = range.split("\\s+");
            int startHour = 0, endHour = 23;
            if (parts.length >= 2) {
                final var timePattern = Pattern.compile("(\\d{1,2}):\\d{2}");
                var m = timePattern.matcher(parts[0]);
                if (m.find()) startHour = Integer.parseInt(m.group(1));
                m = timePattern.matcher(parts[parts.length - 1]);
                if (m.find()) endHour = Integer.parseInt(m.group(1));
            }
            for (int h = 0; h < 24; h++) {
                int count = (h >= startHour && h <= endHour) ? 1 : 0;
                series.getData().add(new XYChart.Data<>(String.format("%02d:00", h), count));
            }
        } catch (final Exception e) {
            for (int h = 0; h < 24; h++) {
                series.getData().add(new XYChart.Data<>(String.format("%02d:00", h), 0));
            }
        }
        flightsPerHourChart.getData().add(series);
    }

    @FXML
    private void onSaveReport() {
        final var content = reportOutput.getText();
        if (content == null || content.isBlank()) return;
        final var chooser = new FileChooser();
        chooser.setTitle("Save Simulation Report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        chooser.setInitialFileName("simulation_report.txt");
        final var file = chooser.showSaveDialog(null);
        if (file != null) {
            try {
                Files.writeString(file.toPath(), content);
                reportOutput.setText("Report saved to: " + file.getAbsolutePath() + "\n\n" + content);
            } catch (final IOException e) {
                reportOutput.setText("Error saving: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onZoomReport() {
        final String text = reportOutput.getText();
        if (text == null || text.isBlank()) return;

        final var area = new TextArea(text);
        area.setEditable(false);
        area.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #e6edf3;"
                + "-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px;");
        area.setWrapText(false);

        final var root = new BorderPane(area);
        root.setStyle("-fx-background-color: #0d1117;");

        final var stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        final var iconStream = SceneManager.class.getResourceAsStream("/icons/aisafe-icon.png");
        if (iconStream != null) {
            stage.getIcons().add(new Image(iconStream));
        }
        stage.setTitle("Simulation Report \u2014 Zoom View");
        stage.setMaximized(true);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.setScene(new Scene(root, 800, 600));
        stage.getScene().getStylesheets().add("/styles/dark-theme.css");
        stage.show();
    }

    @FXML
    private void generateReport() {
        final var selected = simulationSelector.getValue();
        if (selected == null) { reportOutput.setText("Select a simulation first."); return; }
        try {
            final var sim = sims.stream()
                    .filter(s -> s.getDisplayName().equals(selected)).findFirst().orElse(null);
            if (sim != null) {
                final var path = ctrl.generateReport(sim.areaCode());
                reportOutput.setText("Report generated at: " + path + "\n\n" + sim.reportContent());
            }
        } catch (final Exception e) {
            reportOutput.setText("Error: " + e.getMessage());
        }
    }

    private record CReportParseResult(int totalFlights, int totalViolations,
                                       ValidationResult validationResult,
                                       List<String> flightIds,
                                       Map<String, Integer> perFlightViolations,
                                       List<Integer> trend) {}

    public static class SimulationRow {
        private final Simulation sim;
        public SimulationRow(final Simulation s) { this.sim = s; }
        public String getDisplayName() { return sim.toString(); }
        public String areaCode() { return sim.areaCode().toString(); }
        public String timeRange() { return sim.timeRange().toString(); }
        public String thresholdValue() { return String.valueOf(sim.safetyThreshold().value()); }
        public String thresholdUnit() { return sim.safetyThreshold().unit(); }
        public ValidationResult validationResult() { return sim.validationResult(); }
        public String reportContent() { return sim.report().content(); }
    }
}
