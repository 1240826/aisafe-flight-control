package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.manufacturer.application.RegisterManufacturerController;
import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.aisafe.ui.jfx.util.FieldValidator;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.stream.StreamSupport;

public class ManufacturerController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<ManufacturerRow> manufacturersTable;

    @FXML
    private TableColumn<ManufacturerRow, String> colName;

    @FXML
    private TableColumn<ManufacturerRow, String> colCountry;

    @FXML
    private TextField newName;

    @FXML
    private TextField newCountry;

    @FXML
    private Label newNameError;

    @FXML
    private Label newCountryError;

    private final RegisterManufacturerController ctrl = new RegisterManufacturerController();
    private final ObservableList<ManufacturerRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colName.setCellValueFactory(d -> d.getValue().name);
        colCountry.setCellValueFactory(d -> d.getValue().country);

        FieldValidator.onRequired(newName, newNameError, "Manufacturer name");

        searchField.textProperty().addListener((o, a, b) -> refreshTable());

        refreshTable();
    }

    @FXML
    private void refreshTable() {
        items.clear();
        final String searchText = searchField.getText();
        StreamSupport.stream(ctrl.allManufacturers().spliterator(), false)
                .filter(m -> searchText == null || searchText.isBlank()
                        || m.identity().toString().toLowerCase().contains(searchText.toLowerCase())
                        || (m.country() != null && m.country().toLowerCase().contains(searchText.toLowerCase())))
                .forEach(m -> items.add(new ManufacturerRow(
                        m.identity().toString(),
                        m.country()
                )));
        manufacturersTable.setItems(items);
    }

    @FXML
    private void addManufacturer() {
        if (!FieldValidator.isFormValid(newNameError)) {
            NotificationManager.error("Validation Error", "Fix the highlighted fields before submitting.");
            return;
        }
        try {
            final var name = newName.getText();
            final var country = newCountry.getText();
            ctrl.registerManufacturer(name, country.isBlank() ? null : country);
            NotificationManager.success("Manufacturer Registered", "Manufacturer registered successfully!");
            newName.clear();
            newCountry.clear();
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void removeManufacturer() {
        final var selected = manufacturersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationManager.error("No Selection", "Select a manufacturer to remove.");
            return;
        }
        try {
            ctrl.removeManufacturer(selected.name.get());
            NotificationManager.success("Manufacturer Removed", selected.name.get() + " removed successfully.");
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void onZoom() {
        TableZoomUtil.openZoom(manufacturersTable, "Manufacturers");
    }

    public static class ManufacturerRow {
        public final SimpleStringProperty name;
        public final SimpleStringProperty country;

        public ManufacturerRow(final String n, final String c) {
            name = new SimpleStringProperty(n);
            country = new SimpleStringProperty(c);
        }
    }
}
