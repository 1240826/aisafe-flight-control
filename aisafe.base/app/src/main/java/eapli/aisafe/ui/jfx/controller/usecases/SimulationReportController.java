package eapli.aisafe.ui.jfx.controller.usecases;

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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;
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
    private final ObservableList<SimulationRow> sims = FXCollections.observableArrayList();
    private Timeline autoRefresh;
    private boolean liveMode = false;

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

        final var violations = parseViolations(reportText);
        final var flights = parseFlights(reportText);
        final var companies = parseCompanies(reportText);
        final var hourly = parseFlightsPerHour(reportText);
        final var trend = parseViolationsTrend(reportText);

        updateMetrics(flights.size(), violations.size(), companies.size(), sim.validationResult());
        buildViolationsChart(violations);
        buildValidationPieChart(sim.validationResult());
        buildViolationsTrendChart(trend);
        buildCompanyChart(companies, flights);
        buildFlightsPerHourChart(hourly);
    }

    private void updateMetrics(final int totalFlights, final int totalViolations,
                                final int totalCompanies, final ValidationResult result) {
        metricFlights.setText(String.valueOf(totalFlights));
        metricViolations.setText(String.valueOf(totalViolations));
        metricCompanies.setText(String.valueOf(totalCompanies));
        metricPassRate.setText(result == ValidationResult.PASSED ? "100%" :
                result == ValidationResult.FAILED ? "0%" : "Pending");
    }

    private void buildViolationsChart(final Map<String, Integer> violations) {
        violationsChart.getData().clear();
        final var series = new XYChart.Series<String, Number>();
        series.setName("Violations");
        if (violations.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No Violations", 0));
        } else {
            violations.forEach((type, count) ->
                    series.getData().add(new XYChart.Data<>(type, count)));
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
        final var proximitySeries = new XYChart.Series<Number, Number>();
        proximitySeries.setName("Proximity");
        final var altitudeSeries = new XYChart.Series<Number, Number>();
        altitudeSeries.setName("Altitude");

        for (int i = 0; i < trend.size(); i++) {
            final var step = trend.get(i);
            proximitySeries.getData().add(new XYChart.Data<>(i, step));
        }
        violationsTrendChart.getData().addAll(proximitySeries, altitudeSeries);
    }

    private void buildCompanyChart(final Map<String, Integer> companies,
                                    final Map<String, Integer> flights) {
        companyChart.getData().clear();
        final var series = new XYChart.Series<String, Number>();
        series.setName("Flights");
        if (companies.isEmpty() && !flights.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Unknown", flights.size()));
        } else {
            companies.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
        }
        companyChart.getData().add(series);
    }

    private void buildFlightsPerHourChart(final Map<Integer, Integer> hourly) {
        flightsPerHourChart.getData().clear();
        final var series = new XYChart.Series<String, Number>();
        series.setName("Flights");
        for (int h = 0; h < 24; h++) {
            final var count = hourly.getOrDefault(h, 0);
            series.getData().add(new XYChart.Data<>(String.format("%02d:00", h), count));
        }
        flightsPerHourChart.getData().add(series);
    }

    @FXML
    private void onZoomReport() {
        final TextArea copy = new TextArea(reportOutput.getText());
        copy.setEditable(false);
        copy.setStyle(reportOutput.getStyle());
        copy.setWrapText(true);

        final BorderPane root = new BorderPane(copy);
        root.setStyle("-fx-background-color: #0d1117;");

        final Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        final var iconStream = SceneManager.class.getResourceAsStream("/icons/aisafe-icon.png");
        if (iconStream != null) {
            stage.getIcons().add(new Image(iconStream));
        }
        stage.setTitle("Report Output — Zoom View");
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

    private Map<String, Integer> parseViolations(final String content) {
        final Map<String, Integer> map = new LinkedHashMap<>();
        if (content == null) return map;
        for (final var line : content.lines().toList()) {
            final var lower = line.toLowerCase();
            if (lower.contains("proximity") || lower.contains("close")) map.merge("Proximity", 1, Integer::sum);
            else if (lower.contains("altitude")) map.merge("Altitude", 1, Integer::sum);
            else if (lower.contains("fuel")) map.merge("Fuel", 1, Integer::sum);
            else if (lower.contains("violation") || lower.contains("conflict")) map.merge("Other", 1, Integer::sum);
        }
        if (map.isEmpty()) map.put("No Violations", 0);
        return map;
    }

    private Map<String, Integer> parseCompanies(final String content) {
        final Map<String, Integer> map = new LinkedHashMap<>();
        if (content == null) return map;
        for (final var line : content.lines().toList()) {
            final var lower = line.toLowerCase();
            if (lower.contains("company:") || lower.contains("airline:")) {
                final var parts = line.split(":");
                if (parts.length >= 2) {
                    final var company = parts[1].trim().replaceAll("[\\[\\]{}()]", "");
                    if (!company.isEmpty()) map.merge(company.toUpperCase(), 1, Integer::sum);
                }
            }
            for (final var known : KNOWN_COMPANIES) {
                if (lower.contains(known.toLowerCase())) {
                    map.merge(known, 1, Integer::sum);
                    break;
                }
            }
        }
        return map;
    }

    private Map<String, Integer> parseFlights(final String content) {
        final Map<String, Integer> map = new LinkedHashMap<>();
        if (content == null) return map;
        for (final var line : content.lines().toList()) {
            if (line.matches(".*[A-Z]{2}\\d{3,4}.*") || line.toLowerCase().contains("flight:")) {
                map.merge("flights", 1, Integer::sum);
            }
        }
        return map;
    }

    private Map<Integer, Integer> parseFlightsPerHour(final String content) {
        final Map<Integer, Integer> map = new HashMap<>();
        if (content == null) return map;
        for (final var line : content.lines().toList()) {
            if (line.matches(".*\\d{1,2}:\\d{2}.*")) {
                try {
                    final var matcher = java.util.regex.Pattern.compile("(\\d{1,2}):\\d{2}").matcher(line);
                    if (matcher.find()) {
                        final var hour = Integer.parseInt(matcher.group(1));
                        if (hour >= 0 && hour < 24) map.merge(hour, 1, Integer::sum);
                    }
                } catch (final Exception ignored) {}
            }
        }
        return map;
    }

    private List<Integer> parseViolationsTrend(final String content) {
        final List<Integer> trend = new ArrayList<>();
        if (content == null) return trend;
        for (final var line : content.lines().toList()) {
            final var lower = line.toLowerCase();
            if (lower.contains("step:") || lower.contains("time step") || lower.contains("t=")) {
                int count = 0;
                if (lower.contains("violation") || lower.contains("conflict")) count++;
                trend.add(count);
            }
        }
        if (trend.isEmpty()) trend.add(0);
        return trend;
    }

    private static final List<String> KNOWN_COMPANIES = List.of(
            "TAP", "RYANAIR", "BRITISH AIRWAYS", "LUFTHANSA",
            "AIR FRANCE", "IBERIA", "AMERICAN AIRLINES", "DELTA", "UNITED", "KLM"
    );

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
