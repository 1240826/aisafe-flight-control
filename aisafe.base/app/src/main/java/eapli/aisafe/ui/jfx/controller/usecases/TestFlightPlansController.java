package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.flightplan.application.ReportParser;
import eapli.aisafe.flightplan.application.TestFlightPlanController;
import eapli.aisafe.simulation.application.SaveSimulationController;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class TestFlightPlansController {

    private static final String PREFS_HOST = "simulator.host";
    private static final String PREFS_PORT = "simulator.port";
    private static final String PREFS_TIMEOUT = "simulator.timeout";

    @FXML
    private TableView<DraftEntryRow> testPlansTable;

    @FXML
    private TableColumn<DraftEntryRow, String> colSel;

    @FXML
    private TableColumn<DraftEntryRow, String> colNum;

    @FXML
    private TableColumn<DraftEntryRow, String> colFlight;

    @FXML
    private TableColumn<DraftEntryRow, String> colPlanId;

    @FXML
    private TableColumn<DraftEntryRow, String> colRoute;

    @FXML
    private VBox simulationResult;

    @FXML
    private Label simulationTitle;

    @FXML
    private TextArea simulationReport;

    private final TestFlightPlanController testCtrl = new TestFlightPlanController();
    private final SaveSimulationController saveCtrl = new SaveSimulationController();

    private List<DraftEntryRow> currentRows;

    private static final class DraftEntryRow {
        final int index;
        final TestFlightPlanController.FlightPlanEntry entry;
        final BooleanProperty selected = new SimpleBooleanProperty(false);

        DraftEntryRow(final int index, final TestFlightPlanController.FlightPlanEntry entry) {
            this.index = index;
            this.entry = entry;
        }

        String flight() { return entry.flight().identity().toString(); }
        String planId() { return entry.flightPlan().identity().toString(); }
        String route() {
            final var rn = entry.flight().routeName();
            return rn != null ? rn.toString() : "\u2014";
        }
    }

    @FXML
    private void initialize() {
        setupTable();
        refreshTestPlans();
    }

    private void setupTable() {
        testPlansTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        colSel.setCellValueFactory(d -> javafx.beans.binding.Bindings.createStringBinding(
                () -> d.getValue().selected.get() ? "\u2611" : "\u2610",
                d.getValue().selected));

        colNum.setCellValueFactory(d -> javafx.beans.binding.Bindings.createStringBinding(
                () -> String.valueOf(d.getValue().index)));

        colFlight.setCellValueFactory(d -> javafx.beans.binding.Bindings.createStringBinding(
                () -> d.getValue().flight()));

        colPlanId.setCellValueFactory(d -> javafx.beans.binding.Bindings.createStringBinding(
                () -> d.getValue().planId()));

        colRoute.setCellValueFactory(d -> javafx.beans.binding.Bindings.createStringBinding(
                () -> d.getValue().route()));

        testPlansTable.setRowFactory(tv -> {
            final var row = new TableRow<DraftEntryRow>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && e.getClickCount() == 2) {
                    testSingle(row.getItem().entry);
                }
            });
            return row;
        });

        testPlansTable.setOnMouseClicked(e -> {
            final var target = e.getTarget();
            if (target instanceof javafx.scene.Node) {
                var p = ((javafx.scene.Node) target).getParent();
                while (p != null && !(p instanceof TableCell)) {
                    p = p.getParent();
                }
                if (p instanceof TableCell cell && cell.getTableColumn() == colSel) {
                    final var r = cell.getTableRow();
                    if (r != null && !r.isEmpty()) {
                        ((DraftEntryRow) r.getItem()).selected.set(
                                !((DraftEntryRow) r.getItem()).selected.get());
                        e.consume();
                    }
                }
            }
        });
    }

    @FXML
    private void onRefresh() {
        refreshTestPlans();
    }

    private void refreshTestPlans() {
        try {
            final var entries = testCtrl.allDraftEntries();
            currentRows = new ArrayList<>();
            int idx = 1;
            for (final var entry : entries) {
                currentRows.add(new DraftEntryRow(idx++, entry));
            }
            testPlansTable.setItems(FXCollections.observableArrayList(currentRows));
        } catch (final Exception e) {
            testPlansTable.setItems(FXCollections.observableArrayList());
        }
    }

    private List<TestFlightPlanController.FlightPlanEntry> collectSelected() {
        if (currentRows == null) return List.of();
        return currentRows.stream()
                .filter(r -> r.selected.get())
                .map(r -> r.entry)
                .collect(Collectors.toList());
    }

    @FXML
    private void onTestSelected() {
        final var selected = collectSelected();
        if (selected.isEmpty()) {
            NotificationManager.error("No Selection",
                    "Check the \u2713 column for the flight plans you want to test.");
            return;
        }
        runTest(selected);
    }

    private void testSingle(final TestFlightPlanController.FlightPlanEntry entry) {
        runTest(List.of(entry));
    }

    private void applySimulatorSettings() {
        final var prefs = Preferences.userNodeForPackage(SettingsController.class);
        final String host = prefs.get(PREFS_HOST, "");
        final String port = prefs.get(PREFS_PORT, "9999");
        final String timeout = prefs.get(PREFS_TIMEOUT, "180");
        if (!host.isEmpty()) {
            System.setProperty("aisafe.simulator.host", host);
        }
        System.setProperty("aisafe.simulator.port", port);
        System.setProperty("aisafe.simulator.timeout", timeout);
    }

    private void runTest(final List<TestFlightPlanController.FlightPlanEntry> entries) {
        final int total = entries.size();
        simulationResult.setVisible(true);
        simulationResult.setManaged(true);

        final String flightLabel = total == 1
                ? entries.get(0).flight().identity().toString()
                : total + " flights";
        simulationTitle.setText("\u23F3 Testing " + flightLabel + "...");
        simulationTitle.setStyle("-fx-text-fill: #d29922;");
        simulationReport.setText("Connecting to simulator...");

        applySimulatorSettings();

        Platform.runLater(() -> {
            try {
                final var scenario = testCtrl.testScenario(entries);
                final boolean passed = scenario.passed();
                final String msg = scenario.message();
                final String report = scenario.reportContent();

                if (passed) {
                    simulationTitle.setText("\u2713 " + flightLabel + " PASSED");
                    simulationTitle.setStyle("-fx-text-fill: #3fb950;");
                } else {
                    simulationTitle.setText("\u2715 " + flightLabel + " FAILED");
                    simulationTitle.setStyle("-fx-text-fill: #f85149;");
                }

                final var reportText = new StringBuilder();
                reportText.append("=== Test Summary ===\n");
                reportText.append("Selected: ").append(entries.size()).append(" flights\n");
                reportText.append(msg != null ? msg : "No message").append("\n\n");
                if (report != null) {
                    reportText.append("=== Simulator Report ===\n");
                    reportText.append(report);
                }
                simulationReport.setText(reportText.toString());

                // Save Simulation records so results appear in Simulation Reports
                saveSimulationRecords(entries, report, passed);

                NotificationManager.info("Test Complete", msg != null ? msg : "Done");
                refreshTestPlans();
            } catch (final Exception e) {
                simulationTitle.setText("\u2715 Error");
                simulationTitle.setStyle("-fx-text-fill: #f85149;");
                simulationReport.setText("Error: " + e.getMessage() + "\n\n"
                        + "Check that:\n"
                        + "  1. sim_server is running on the VM\n"
                        + "  2. The correct VM IP is configured in Settings\n"
                        + "  3. Port 9999 is not blocked");
                NotificationManager.error("Test Failed", e.getMessage());
            }
        });
    }

    private void saveSimulationRecords(
            final List<TestFlightPlanController.FlightPlanEntry> entries,
            final String reportContent,
            final boolean passed) {
        if (reportContent == null || reportContent.isBlank()) return;
        final var now = LocalDateTime.now();
        final var parsed = ReportParser.parse(reportContent);
        final int violations = parsed.violationCount();

        for (final var entry : entries) {
            try {
                final var parsedFlight = parsed.perFlightResults().stream()
                        .filter(r -> r.flightId().equals(entry.flight().identity().toString()))
                        .findFirst().orElse(null);
                final boolean flightPassed = parsedFlight != null
                        ? parsedFlight.isPassed() : passed;
                final int flightViolations = parsedFlight != null
                        ? parsedFlight.violations() : violations;

                final var area = entry.flight().routeName() != null
                        ? entry.flight().routeName().toString().substring(0,
                                Math.min(3, entry.flight().routeName().toString().length()))
                        : "GEN";

                saveCtrl.saveSimulation(
                        area,
                        now.minusMinutes(5),
                        now,
                        Math.max(1, flightViolations + 1),
                        "violations",
                        "aisafe-test-" + entry.flight().identity() + ".txt",
                        reportContent);
            } catch (final Exception ignored) {
            }
        }
    }

    @FXML
    private void onZoomTable() {
        if (currentRows == null || currentRows.isEmpty()) return;

        final var sb = new StringBuilder();
        final String header = String.format("%-4s %-4s %-16s %-14s %s",
                "#", "Sel", "Flight", "Plan ID", "Route");
        sb.append(header).append("\n");
        sb.append("\u2500".repeat(Math.max(80, header.length()))).append("\n");
        for (final var row : currentRows) {
            final String sel = row.selected.get() ? "[X]" : "[ ]";
            sb.append(String.format("%-4d %-4s %-16s %-14s %s\n",
                    row.index, sel, row.flight(), row.planId(), row.route()));
        }
        showZoomWindow("Test Flight Plans", sb.toString(), false, 700, 500);
    }

    @FXML
    private void onZoomReport() {
        final var text = simulationReport.getText();
        if (text == null || text.isBlank()) return;
        showZoomWindow("Simulation Report", text, true, 800, 600);
    }

    @FXML
    private void onSaveReport() {
        final var text = simulationReport.getText();
        if (text == null || text.isBlank()) return;
        final var chooser = new FileChooser();
        chooser.setTitle("Save Simulation Report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        chooser.setInitialFileName("simulation_report_" + LocalDateTime.now().toString().replace(":", "-") + ".txt");
        final var file = chooser.showSaveDialog(null);
        if (file != null) {
            try {
                Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
                NotificationManager.success("Report Saved", "Saved to: " + file.getAbsolutePath());
            } catch (final IOException e) {
                NotificationManager.error("Save Failed", e.getMessage());
            }
        }
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
