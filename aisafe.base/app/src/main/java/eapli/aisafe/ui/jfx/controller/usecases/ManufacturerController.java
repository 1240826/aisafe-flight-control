package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.manufacturer.application.RegisterManufacturerController;
import eapli.aisafe.manufacturer.domain.Manufacturer;
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

    private final RegisterManufacturerController ctrl = new RegisterManufacturerController();
    private final ObservableList<ManufacturerRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colName.setCellValueFactory(d -> d.getValue().name);
        colCountry.setCellValueFactory(d -> d.getValue().country);
        refreshTable();
    }

    @FXML
    private void refreshTable() {
        items.clear();
        StreamSupport.stream(ctrl.allManufacturers().spliterator(), false)
                .forEach(m -> items.add(new ManufacturerRow(
                        m.identity().toString(),
                        m.country()
                )));
        manufacturersTable.setItems(items);
    }

    @FXML
    private void addManufacturer() {
        try {
            final var name = newName.getText();
            final var country = newCountry.getText();
            if (name.isBlank()) {
                NotificationManager.error("Validation Error", "Manufacturer name is required.");
                return;
            }
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
