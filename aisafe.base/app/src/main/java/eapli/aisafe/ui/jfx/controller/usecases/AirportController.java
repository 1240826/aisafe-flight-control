package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.airport.application.CreateAirportController;
import eapli.aisafe.ui.jfx.util.FormHelper;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;

import java.util.stream.StreamSupport;

public class AirportController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<AirportRow> airportsTable;

    @FXML
    private TableColumn<AirportRow, String> colIata;

    @FXML
    private TableColumn<AirportRow, String> colIcao;

    @FXML
    private TableColumn<AirportRow, String> colName;

    @FXML
    private TableColumn<AirportRow, String> colCity;

    @FXML
    private TableColumn<AirportRow, String> colCountry;

    @FXML
    private TableColumn<AirportRow, String> colArea;

    @FXML
    private TextField newIata;

    @FXML
    private TextField newIcao;

    @FXML
    private TextField newName;

    @FXML
    private TextField newCity;

    @FXML
    private TextField newCountry;

    @FXML
    private TextField newLatitude;

    @FXML
    private TextField newLongitude;

    @FXML
    private TextField newElevation;

    @FXML
    private ComboBox<String> newArea;

    private final CreateAirportController ctrl = new CreateAirportController();
    private final ObservableList<AirportRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colIata.setCellValueFactory(d -> d.getValue().iata);
        colIcao.setCellValueFactory(d -> d.getValue().icao);
        colName.setCellValueFactory(d -> d.getValue().name);
        colCity.setCellValueFactory(d -> d.getValue().city);
        colCountry.setCellValueFactory(d -> d.getValue().country);
        colArea.setCellValueFactory(d -> d.getValue().area);

        airportsTable.setEditable(true);

        searchField.textProperty().addListener((obs, old, text) -> filterTable(text));

        loadAreas();
        refreshTable();

        FormHelper.addNumericConstraint(newLatitude);
        FormHelper.addNumericConstraint(newLongitude);
        FormHelper.addNumericConstraint(newElevation);
    }

    private void loadAreas() {
        try {
            final var areas = new eapli.aisafe.aircontrolarea.application
                    .RegisterAirControlAreaController().allAirControlAreas();
            StreamSupport.stream(areas.spliterator(), false)
                    .forEach(a -> newArea.getItems().add(a.identity().toString()));
        } catch (final Exception e) {
            newArea.getItems().add("No areas registered");
        }
        if (!newArea.getItems().isEmpty()) {
            newArea.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void refreshTable() {
        items.clear();
        StreamSupport.stream(ctrl.allAirports().spliterator(), false)
                .forEach(a -> items.add(new AirportRow(
                        a.iata().toString(),
                        a.icao().toString(),
                        a.name(),
                        a.city(),
                        a.country(),
                        a.areaCode().toString()
                )));
        airportsTable.setItems(items);
    }

    private void filterTable(final String text) {
        if (text == null || text.isBlank()) {
            airportsTable.setItems(items);
            return;
        }
        final var filtered = FXCollections.observableArrayList(
                items.filtered(row -> row.matches(text.toLowerCase()))
        );
        airportsTable.setItems(filtered);
    }

    @FXML
    private void addAirport() {
        try {
            final String iata = newIata.getText().trim();
            final String icao = newIcao.getText().trim();
            final String name = newName.getText().trim();
            if (iata.isBlank() || icao.isBlank() || name.isBlank()) {
                NotificationManager.error("Validation Error", "IATA, ICAO, and Airport Name are required.");
                return;
            }
            ctrl.createAirport(
                    iata, icao, name,
                    newCity.getText().trim(), newCountry.getText().trim(),
                    parseDouble(newLatitude), parseDouble(newLongitude),
                    parseDouble(newElevation) != null ? parseDouble(newElevation) : 0.0,
                    "meters",
                    newArea.getValue()
            );
            NotificationManager.success("Airport Created", name + " (" + iata + ") registered successfully.");
            clearForm();
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Registration Failed", e.getMessage());
        }
    }

    private void clearForm() {
        newIata.clear(); newIcao.clear(); newName.clear();
        newCity.clear(); newCountry.clear();
        newLatitude.clear(); newLongitude.clear(); newElevation.clear();
    }

    private Double parseDouble(final TextField field) {
        final var val = field.getText();
        if (val == null || val.isBlank()) return null;
        try { return Double.parseDouble(val); } catch (final NumberFormatException e) { return null; }
    }

    @FXML
    private void onZoom() {
        TableZoomUtil.openZoom(airportsTable, "Airports");
    }

    public static class AirportRow {
        public final SimpleStringProperty iata;
        public final SimpleStringProperty icao;
        public final SimpleStringProperty name;
        public final SimpleStringProperty city;
        public final SimpleStringProperty country;
        public final SimpleStringProperty area;

        public AirportRow(final String i, final String ica, final String n,
                          final String ci, final String co, final String a) {
            iata = new SimpleStringProperty(i);
            icao = new SimpleStringProperty(ica);
            name = new SimpleStringProperty(n);
            city = new SimpleStringProperty(ci);
            country = new SimpleStringProperty(co);
            area = new SimpleStringProperty(a);
        }

        public boolean matches(final String query) {
            return iata.get().toLowerCase().contains(query)
                    || icao.get().toLowerCase().contains(query)
                    || name.get().toLowerCase().contains(query)
                    || city.get().toLowerCase().contains(query)
                    || country.get().toLowerCase().contains(query)
                    || area.get().toLowerCase().contains(query);
        }
    }
}
