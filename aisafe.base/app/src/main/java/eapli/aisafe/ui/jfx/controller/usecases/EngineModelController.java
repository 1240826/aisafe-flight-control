package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.enginemodel.application.CreateEngineModelController;
import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.aisafe.ui.jfx.util.FieldValidator;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.stream.StreamSupport;

public class EngineModelController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<EngineRow> enginesTable;

    @FXML
    private TableColumn<EngineRow, String> colName;

    @FXML
    private TableColumn<EngineRow, String> colManufacturer;

    @FXML
    private TableColumn<EngineRow, String> colType;

    @FXML
    private TableColumn<EngineRow, String> colThrust;

    @FXML
    private TableColumn<EngineRow, String> colFuel;

    @FXML
    private TextField newName;

    @FXML
    private ComboBox<String> newManufacturer;

    @FXML
    private ComboBox<MotorizationType> newMotorization;

    @FXML
    private Label newCodeError;

    @FXML
    private Label newNameError;

    @FXML
    private Label newManufacturerError;

    @FXML
    private Label newMotorizationError;

    @FXML
    private Label newFuelTypeError;

    @FXML
    private Label newPowerError;

    @FXML
    private Label newStaticThrustError;

    @FXML
    private Label newCruiseThrustError;

    @FXML
    private Label newTsfcError;

    @FXML
    private TextField newStaticThrust;

    @FXML
    private TextField newCode;

    @FXML
    private ComboBox<String> newFuelType;

    @FXML
    private TextField newPower;

    @FXML
    private TextField newCruiseThrust;

    @FXML
    private TextField newTsfc;

    private final CreateEngineModelController ctrl = new CreateEngineModelController();
    private final ObservableList<EngineRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colName.setCellValueFactory(d -> d.getValue().name);
        colManufacturer.setCellValueFactory(d -> d.getValue().manufacturer);
        colType.setCellValueFactory(d -> d.getValue().type);
        colThrust.setCellValueFactory(d -> d.getValue().thrust);
        colFuel.setCellValueFactory(d -> d.getValue().fuel);

        newMotorization.getItems().addAll(MotorizationType.values());
        newMotorization.getSelectionModel().selectFirst();

        FieldValidator.onRequired(newCode, newCodeError, "Engine code");
        FieldValidator.onRequired(newName, newNameError, "Engine name");
        FieldValidator.onRequiredCombo(newManufacturer, newManufacturerError, "Manufacturer");
        FieldValidator.onNumeric(newPower, newPowerError, "Power");
        FieldValidator.onNumeric(newStaticThrust, newStaticThrustError, "Static thrust");
        FieldValidator.onNumeric(newCruiseThrust, newCruiseThrustError, "Cruise thrust");
        FieldValidator.onNumeric(newTsfc, newTsfcError, "TSFC");

        loadManufacturers();
        loadFuelTypes();

        searchField.textProperty().addListener((o, a, b) -> refreshTable());

        refreshTable();
    }

    private void loadManufacturers() {
        try {
            StreamSupport.stream(ctrl.allManufacturers().spliterator(), false)
                    .forEach(m -> newManufacturer.getItems().add(m.identity().toString()));
        } catch (final Exception e) {
            NotificationManager.error("Error", "Could not load manufacturers: " + e.getMessage());
        }
        if (!newManufacturer.getItems().isEmpty()) {
            newManufacturer.getSelectionModel().selectFirst();
        }
    }

    private void loadFuelTypes() {
        newFuelType.getItems().addAll("Jet-A1", "Jet-A", "Jet-B", "AvGas", "Biodiesel", "Electric", "Hydrogen");
        newFuelType.getSelectionModel().selectFirst();
    }

    @FXML
    private void refreshTable() {
        items.clear();
        final String searchText = searchField.getText();
        StreamSupport.stream(ctrl.allEngineModels().spliterator(), false)
                .filter(e -> searchText == null || searchText.isBlank()
                        || e.identity().toString().toLowerCase().contains(searchText.toLowerCase())
                        || e.engineName().toString().toLowerCase().contains(searchText.toLowerCase())
                        || e.manufacturerName().toLowerCase().contains(searchText.toLowerCase())
                        || e.motorizationType().toString().toLowerCase().contains(searchText.toLowerCase())
                        || e.fuelType().toLowerCase().contains(searchText.toLowerCase()))
                .forEach(e -> items.add(new EngineRow(
                        e.identity().toString(),
                        e.engineName().toString(),
                        e.manufacturerName(),
                        e.motorizationType().toString(),
                        e.fuelType(),
                        e.staticThrust().toString()
                )));
        enginesTable.setItems(items);
    }

    @FXML
    private void addEngine() {
        if (!FieldValidator.isFormValid(newCodeError, newNameError, newManufacturerError,
                newMotorizationError, newFuelTypeError, newPowerError,
                newStaticThrustError, newCruiseThrustError, newTsfcError)) {
            NotificationManager.error("Validation Error", "Fix the highlighted fields before submitting.");
            return;
        }
        try {
            final var name = newName.getText();
            final var manufacturer = newManufacturer.getValue();
            final var motorization = newMotorization.getValue();
            final var thrustStr = newStaticThrust.getText();
            final var fuel = newFuelType.getValue();
            final var code = newCode.getText();
            final var powerStr = newPower.getText();
            final var cruiseStr = newCruiseThrust.getText();
            final var tsfcStr = newTsfc.getText();

            ctrl.createEngineModel(
                    code, name, manufacturer,
                    fuel != null ? fuel : "Jet-A1",
                    motorization,
                    powerStr.isBlank() ? 0 : Double.parseDouble(powerStr), "kW",
                    thrustStr.isBlank() ? 0 : Double.parseDouble(thrustStr), "kN",
                    cruiseStr.isBlank() ? 0 : Double.parseDouble(cruiseStr),
                    tsfcStr.isBlank() ? 0 : Double.parseDouble(tsfcStr), "kg/(kN·h)");

            NotificationManager.success("Engine Created", "Engine model created successfully!");
            newCode.clear();
            newName.clear();
            newStaticThrust.clear();
            newPower.clear();
            newCruiseThrust.clear();
            newTsfc.clear();
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void onZoom() {
        TableZoomUtil.openZoom(enginesTable, "Engine Models");
    }

    public static class EngineRow {
        public final SimpleStringProperty name;
        public final SimpleStringProperty manufacturer;
        public final SimpleStringProperty type;
        public final SimpleStringProperty thrust;
        public final SimpleStringProperty fuel;

        public EngineRow(final String c, final String n, final String m, final String t, final String f, final String th) {
            name = new SimpleStringProperty(n);
            manufacturer = new SimpleStringProperty(m);
            type = new SimpleStringProperty(t);
            thrust = new SimpleStringProperty(th);
            fuel = new SimpleStringProperty(f);
        }
    }
}
