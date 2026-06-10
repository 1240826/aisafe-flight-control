package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.aircontrolarea.application.RegisterAirControlAreaController;
import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.stream.StreamSupport;

public class AirControlAreaController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<AreaRow> areasTable;

    @FXML
    private TableColumn<AreaRow, String> colCode;

    @FXML
    private TableColumn<AreaRow, String> colName;

    @FXML
    private TextField newCode;

    @FXML
    private TextField newName;

    @FXML
    private TextField newMinLat;

    @FXML
    private TextField newMaxLat;

    @FXML
    private TextField newMinLon;

    @FXML
    private TextField newMaxLon;

    private final RegisterAirControlAreaController ctrl = new RegisterAirControlAreaController();
    private final ObservableList<AreaRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colCode.setCellValueFactory(d -> d.getValue().code);
        colName.setCellValueFactory(d -> d.getValue().name);
        refreshTable();
    }

    @FXML
    private void refreshTable() {
        items.clear();
        StreamSupport.stream(ctrl.allAirControlAreas().spliterator(), false)
                .forEach(a -> items.add(new AreaRow(
                        a.identity().toString(),
                        a.name().toString()
                )));
        areasTable.setItems(items);
    }

    @FXML
    private void addArea() {
        try {
            if (newCode.getText().isBlank() || newName.getText().isBlank()) {
                NotificationManager.error("Validation Error", "Code and name are required.");
                return;
            }
            ctrl.registerAirControlArea(
                    newCode.getText(),
                    newName.getText(),
                    parseDoubleSafe(newMinLat),
                    parseDoubleSafe(newMaxLat),
                    parseDoubleSafe(newMinLon),
                    parseDoubleSafe(newMaxLon)
            );
            NotificationManager.success("Area Created", "Air control area registered!");
            newCode.clear();
            newName.clear();
            newMinLat.clear();
            newMaxLat.clear();
            newMinLon.clear();
            newMaxLon.clear();
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    private Double parseDoubleSafe(final TextField field) {
        final var val = field.getText();
        if (val == null || val.isBlank()) return null;
        try { return Double.parseDouble(val); } catch (final NumberFormatException e) { return null; }
    }

    @FXML
    private void onZoom() {
        TableZoomUtil.openZoom(areasTable, "Air Control Areas");
    }

    public static class AreaRow {
        public final SimpleStringProperty code;
        public final SimpleStringProperty name;

        public AreaRow(final String c, final String n) {
            code = new SimpleStringProperty(c);
            name = new SimpleStringProperty(n);
        }
    }
}
