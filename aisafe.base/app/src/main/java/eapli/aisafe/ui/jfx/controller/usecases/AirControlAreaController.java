package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.aircontrolarea.application.RegisterAirControlAreaController;
import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.ui.jfx.util.FieldValidator;
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
    private TableColumn<AreaRow, String> colMinLat;

    @FXML
    private TableColumn<AreaRow, String> colMaxLat;

    @FXML
    private TableColumn<AreaRow, String> colMinLon;

    @FXML
    private TableColumn<AreaRow, String> colMaxLon;

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

    @FXML
    private Label newCodeError;

    @FXML
    private Label newNameError;

    @FXML
    private Label newMinLatError;

    @FXML
    private Label newMaxLatError;

    @FXML
    private Label newMinLonError;

    @FXML
    private Label newMaxLonError;

    private final RegisterAirControlAreaController ctrl = new RegisterAirControlAreaController();
    private final ObservableList<AreaRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colCode.setCellValueFactory(d -> d.getValue().code);
        colName.setCellValueFactory(d -> d.getValue().name);
        colMinLat.setCellValueFactory(d -> d.getValue().minLat);
        colMaxLat.setCellValueFactory(d -> d.getValue().maxLat);
        colMinLon.setCellValueFactory(d -> d.getValue().minLon);
        colMaxLon.setCellValueFactory(d -> d.getValue().maxLon);

        FieldValidator.onRequired(newCode, newCodeError, "Area code");
        FieldValidator.onMinLength(newCode, newCodeError, 3, "Area code");
        FieldValidator.onRequired(newName, newNameError, "Area name");
        FieldValidator.onNumeric(newMinLat, newMinLatError, "Min latitude");
        FieldValidator.onNumeric(newMaxLat, newMaxLatError, "Max latitude");
        FieldValidator.onNumeric(newMinLon, newMinLonError, "Min longitude");
        FieldValidator.onNumeric(newMaxLon, newMaxLonError, "Max longitude");

        searchField.textProperty().addListener((o, a, b) -> refreshTable());

        refreshTable();
    }

    @FXML
    private void refreshTable() {
        items.clear();
        final String searchText = searchField.getText();
        StreamSupport.stream(ctrl.allAirControlAreas().spliterator(), false)
                .filter(a -> searchText == null || searchText.isBlank()
                        || a.identity().toString().toLowerCase().contains(searchText.toLowerCase())
                        || a.name().toString().toLowerCase().contains(searchText.toLowerCase()))
                .forEach(a -> items.add(new AreaRow(
                        a.identity().toString(),
                        a.name().toString(),
                        String.valueOf(a.minLat()),
                        String.valueOf(a.maxLat()),
                        String.valueOf(a.minLon()),
                        String.valueOf(a.maxLon())
                )));
        areasTable.setItems(items);
    }

    @FXML
    private void addArea() {
        if (!FieldValidator.isFormValid(newCodeError, newNameError, newMinLatError, newMaxLatError, newMinLonError, newMaxLonError)) {
            NotificationManager.error("Validation Error", "Fix the highlighted fields before submitting.");
            return;
        }
        try {
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
        public final SimpleStringProperty minLat;
        public final SimpleStringProperty maxLat;
        public final SimpleStringProperty minLon;
        public final SimpleStringProperty maxLon;

        public AreaRow(final String c, final String n, final String minLat, final String maxLat, final String minLon, final String maxLon) {
            code = new SimpleStringProperty(c);
            name = new SimpleStringProperty(n);
            this.minLat = new SimpleStringProperty(minLat);
            this.maxLat = new SimpleStringProperty(maxLat);
            this.minLon = new SimpleStringProperty(minLon);
            this.maxLon = new SimpleStringProperty(maxLon);
        }
    }
}
