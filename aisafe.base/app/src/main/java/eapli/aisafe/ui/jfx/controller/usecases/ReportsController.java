package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.report.application.GenerateMonthlyReportController;
import eapli.aisafe.report.domain.MonthlyReport;
import eapli.aisafe.ui.jfx.SceneManager;
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

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportsController {

    @FXML
    private DatePicker monthPicker;

    @FXML
    private ComboBox<String> reportTypeCombo;

    @FXML
    private TextArea reportOutput;

    @FXML
    private VBox metricsSection;

    @FXML
    private Label metricTotalFlights;

    @FXML
    private Label metricPassRate;

    @FXML
    private Label metricActivePilots;

    @FXML
    private Label metricAircraft;

    @FXML
    private Label metricWeather;

    @FXML
    private VBox chartsSection;

    @FXML
    private BarChart<String, Number> statusChart;

    @FXML
    private CategoryAxis statusXAxis;

    @FXML
    private NumberAxis statusYAxis;

    @FXML
    private LineChart<Number, Number> passRateChart;

    @FXML
    private NumberAxis passRateXAxis;

    @FXML
    private NumberAxis passRateYAxis;

    @FXML
    private PieChart distributionPieChart;

    @FXML
    private BarChart<String, Number> weeklyChart;

    @FXML
    private CategoryAxis weeklyXAxis;

    @FXML
    private NumberAxis weeklyYAxis;

    private final GenerateMonthlyReportController ctrl = new GenerateMonthlyReportController();
    private MonthlyReport currentReport;

    @FXML
    private void initialize() {
        reportTypeCombo.getItems().addAll(
                "Monthly Statistics",
                "Compliance Report",
                "Incident Report"
        );
        reportTypeCombo.getSelectionModel().selectFirst();
        monthPicker.setValue(java.time.LocalDate.now());
    }

    @FXML
    private void generateReport() {
        try {
            final var date = monthPicker.getValue();
            if (date == null) {
                reportOutput.setText("Please select a month.");
                return;
            }
            final var month = YearMonth.from(date);
            final var report = ctrl.generateForMonth(month);
            currentReport = report;

            if (report != null) {
                reportOutput.setText(report.toString());
                showCharts(report);
            } else {
                reportOutput.setText("No data available for " + month.format(
                        DateTimeFormatter.ofPattern("MMMM yyyy")) + ".");
                hideCharts();
            }
        } catch (final Exception e) {
            reportOutput.setText("Error generating report: " + e.getMessage());
            hideCharts();
        }
    }

    private void showCharts(final MonthlyReport report) {
        metricsSection.setManaged(true);
        metricsSection.setVisible(true);
        chartsSection.setManaged(true);
        chartsSection.setVisible(true);

        metricTotalFlights.setText(String.valueOf(report.totalFlights()));
        metricPassRate.setText(String.format("%.1f%%", report.passRatePercent()));
        metricActivePilots.setText(String.valueOf(report.totalActivePilots()));
        metricAircraft.setText(String.valueOf(report.totalAircraft()));
        metricWeather.setText(String.valueOf(report.totalWeatherRecords()));

        buildStatusChart(report);
        buildPassRateChart(report);
        buildDistributionPieChart(report);
        buildWeeklyChart(report);
    }

    private void hideCharts() {
        metricsSection.setManaged(false);
        metricsSection.setVisible(false);
        chartsSection.setManaged(false);
        chartsSection.setVisible(false);
    }

    private void buildStatusChart(final MonthlyReport report) {
        statusChart.getData().clear();
        final var series = new XYChart.Series<String, Number>();
        series.setName("Flight Plans");

        final Map<String, Long> data = new LinkedHashMap<>();
        data.put("Draft", report.flightPlansDraft());
        data.put("In Test", report.flightPlansInTest());
        data.put("Passed", report.flightPlansPassed());
        data.put("Failed", report.flightPlansFailed());

        data.forEach((key, value) -> series.getData().add(new XYChart.Data<>(key, value)));
        statusChart.getData().add(series);

        // Apply colors via lookup after rendering
        statusChart.applyCss();
        for (final var d : series.getData()) {
            final var node = d.getNode();
            if (node != null) {
                final var name = d.getXValue();
                if ("Passed".equals(name)) node.setStyle("-fx-bar-fill: #3fb950;");
                else if ("Failed".equals(name)) node.setStyle("-fx-bar-fill: #f85149;");
                else if ("Draft".equals(name)) node.setStyle("-fx-bar-fill: #8b949e;");
                else node.setStyle("-fx-bar-fill: #d29922;");
            }
        }
    }

    private void buildPassRateChart(final MonthlyReport report) {
        passRateChart.getData().clear();
        final var series = new XYChart.Series<Number, Number>();
        series.setName("Pass Rate");

        // Try to get pass rate for the last 6 months
        final var currentMonth = report.period();
        int idx = 0;
        for (int i = 5; i >= 0; i--) {
            final var month = currentMonth.minusMonths(i);
            try {
                final var r = ctrl.generateForMonth(month);
                if (r != null && r.testedFlightPlans() > 0) {
                    series.getData().add(new XYChart.Data<>(idx, r.passRatePercent()));
                } else {
                    series.getData().add(new XYChart.Data<>(idx, 0));
                }
            } catch (final Exception e) {
                series.getData().add(new XYChart.Data<>(idx, 0));
            }
            idx++;
        }
        passRateChart.getData().add(series);

        // Style the line
        series.getNode().setStyle("-fx-stroke: #58a6ff; -fx-stroke-width: 2px;");
    }

    private void buildDistributionPieChart(final MonthlyReport report) {
        distributionPieChart.getData().clear();

        final var draft = new PieChart.Data("Draft", report.flightPlansDraft());
        final var inTest = new PieChart.Data("In Test", report.flightPlansInTest());
        final var passed = new PieChart.Data("Passed", report.flightPlansPassed());
        final var failed = new PieChart.Data("Failed", report.flightPlansFailed());

        distributionPieChart.getData().addAll(draft, inTest, passed, failed);

        distributionPieChart.getData().forEach(d -> {
            final var node = d.getNode();
            if (node != null) {
                switch (d.getName()) {
                    case "Passed" -> node.setStyle("-fx-pie-color: #3fb950;");
                    case "Failed" -> node.setStyle("-fx-pie-color: #f85149;");
                    case "Draft" -> node.setStyle("-fx-pie-color: #8b949e;");
                    case "In Test" -> node.setStyle("-fx-pie-color: #d29922;");
                }
            }
        });
    }

    private void buildWeeklyChart(final MonthlyReport report) {
        weeklyChart.getData().clear();
        final var series = new XYChart.Series<String, Number>();
        series.setName("Flights");

        final var fw = report.flightsPerWeek();
        if (fw != null && !fw.isEmpty()) {
            final var weeks = fw.split("[,\\s]+");
            for (int i = 0; i < weeks.length; i++) {
                try {
                    final var count = Long.parseLong(weeks[i].trim());
                    series.getData().add(new XYChart.Data<>("Week " + (i + 1), count));
                } catch (final NumberFormatException ignored) {
                    series.getData().add(new XYChart.Data<>("Week " + (i + 1), 0));
                }
            }
        }
        // If no weekly data, create placeholder
        if (series.getData().isEmpty()) {
            series.getData().add(new XYChart.Data<>("This Month", report.totalFlights()));
        }
        weeklyChart.getData().add(series);
    }

    @FXML
    private void onZoomReport() {
        final String text = reportOutput.getText();
        if (text == null || text.isBlank()) return;

        final String title = currentReport != null
                ? "Monthly Report \u2014 " + currentReport.period()
                : "Report Output";

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
        stage.setTitle(title);
        stage.setMaximized(true);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.setScene(new Scene(root, 800, 600));
        stage.getScene().getStylesheets().add("/styles/dark-theme.css");
        stage.show();
    }

    @FXML
    private void exportReport() {
        final var content = reportOutput.getText();
        if (content == null || content.isBlank() || content.startsWith("Error") || content.startsWith("No data") || content.startsWith("Please select")) {
            reportOutput.setText("No valid report to export. Generate one first.");
            return;
        }

        final var chooser = new FileChooser();
        chooser.setTitle("Export Report");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        final var file = chooser.showSaveDialog(null);
        if (file != null) {
            try {
                final var sb = new StringBuilder();
                if (currentReport != null) {
                    sb.append("MONTHLY REPORT — ").append(currentReport.period()).append("\n");
                    sb.append("Report Type: ").append(reportTypeCombo.getValue()).append("\n");
                    sb.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n");
                    sb.append("========================================\n\n");
                    sb.append("Total Flights: ").append(currentReport.totalFlights()).append("\n");
                    sb.append("Flight Plans: ").append(currentReport.totalFlightPlans()).append("\n");
                    sb.append("  Draft: ").append(currentReport.flightPlansDraft()).append("\n");
                    sb.append("  In Test: ").append(currentReport.flightPlansInTest()).append("\n");
                    sb.append("  Passed: ").append(currentReport.flightPlansPassed()).append("\n");
                    sb.append("  Failed: ").append(currentReport.flightPlansFailed()).append("\n");
                    sb.append("Pass Rate: ").append(String.format("%.1f%%", currentReport.passRatePercent())).append("\n");
                    sb.append("Weather Records: ").append(currentReport.totalWeatherRecords()).append("\n");
                    sb.append("Active Pilots: ").append(currentReport.totalActivePilots()).append("\n");
                    sb.append("Total Aircraft: ").append(currentReport.totalAircraft()).append("\n");
                } else {
                    sb.append(content);
                }
                java.nio.file.Files.writeString(file.toPath(), sb.toString());
                reportOutput.setText("Report exported to: " + file.getAbsolutePath() + "\n\n" + content);
            } catch (final Exception e) {
                reportOutput.setText("Error exporting: " + e.getMessage());
            }
        }
    }
}
