package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.aircraftmodel.application.CreateAircraftModelController;
import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftType;
import eapli.aisafe.ui.jfx.util.FieldValidator;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.stream.StreamSupport;

public class AircraftModelController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<ModelRow> modelsTable;

    @FXML
    private TableColumn<ModelRow, String> colCode;

    @FXML
    private TableColumn<ModelRow, String> colName;

    @FXML
    private TableColumn<ModelRow, String> colManufacturer;

    @FXML
    private TableColumn<ModelRow, String> colType;

    @FXML
    private TableColumn<ModelRow, String> colPassengers;

    @FXML
    private TableColumn<ModelRow, String> colEngines;

    @FXML
    private TextField newCode;

    @FXML
    private TextField newName;

    @FXML
    private ComboBox<String> newManufacturer;

    @FXML
    private ComboBox<AircraftType> newType;

    @FXML
    private Label newCodeError;

    @FXML
    private Label newNameError;

    @FXML
    private Label newManufacturerError;

    @FXML
    private Label newTypeError;

    @FXML
    private Label newMaxPassengersError;

    @FXML
    private Label newEmptyWeightError;

    @FXML
    private Label newMtowError;

    @FXML
    private Label newMzfwError;

    @FXML
    private Label newFuelCapacityError;

    @FXML
    private Label newCeilingError;

    @FXML
    private Label newCruiseSpeedError;

    @FXML
    private Label newMaxRangeError;

    @FXML
    private Label newWingAreaError;

    @FXML
    private Label newCdError;

    @FXML
    private Label newClError;

    @FXML
    private TextField newMaxPassengers;

    @FXML
    private TextField newMtow;

    @FXML
    private TextField newMzfw;

    @FXML
    private TextField newEmptyWeight;

    @FXML
    private TextField newFuelCapacity;

    @FXML
    private TextField newCruiseSpeed;

    @FXML
    private TextField newCeiling;

    @FXML
    private TextField newMaxRange;

    @FXML
    private TextField newWingArea;

    @FXML
    private TextField newCd;

    @FXML
    private TextField newCl;

    private final CreateAircraftModelController ctrl = new CreateAircraftModelController();
    private final ObservableList<ModelRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colCode.setCellValueFactory(d -> d.getValue().code);
        colName.setCellValueFactory(d -> d.getValue().name);
        colManufacturer.setCellValueFactory(d -> d.getValue().manufacturer);
        colType.setCellValueFactory(d -> d.getValue().type);
        colPassengers.setCellValueFactory(d -> d.getValue().passengers);
        colEngines.setCellValueFactory(d -> d.getValue().engines);

        newType.getItems().addAll(AircraftType.values());
        newType.getSelectionModel().selectFirst();

        FieldValidator.onRequired(newCode, newCodeError, "Model code");
        FieldValidator.onRequired(newName, newNameError, "Model name");
        FieldValidator.onRequiredCombo(newManufacturer, newManufacturerError, "Manufacturer");
        FieldValidator.onNumeric(newMaxPassengers, newMaxPassengersError, "Max passengers");
        FieldValidator.onNumeric(newEmptyWeight, newEmptyWeightError, "Empty weight");
        FieldValidator.onNumeric(newMtow, newMtowError, "MTOW");
        FieldValidator.onNumeric(newMzfw, newMzfwError, "MZFW");
        FieldValidator.onNumeric(newFuelCapacity, newFuelCapacityError, "Fuel capacity");
        FieldValidator.onNumeric(newCeiling, newCeilingError, "Ceiling");
        FieldValidator.onNumeric(newCruiseSpeed, newCruiseSpeedError, "Cruise speed");
        FieldValidator.onNumeric(newMaxRange, newMaxRangeError, "Max range");
        FieldValidator.onNumeric(newWingArea, newWingAreaError, "Wing area");
        FieldValidator.onNumeric(newCd, newCdError, "Drag coefficient");
        FieldValidator.onNumeric(newCl, newClError, "Lift coefficient");

        loadManufacturers();

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

    @FXML
    private void refreshTable() {
        items.clear();
        final String searchText = searchField.getText();
        StreamSupport.stream(ctrl.allAircraftModels().spliterator(), false)
                .filter(m -> searchText == null || searchText.isBlank()
                        || m.identity().toString().toLowerCase().contains(searchText.toLowerCase())
                        || m.name().toLowerCase().contains(searchText.toLowerCase())
                        || m.manufacturerName().toLowerCase().contains(searchText.toLowerCase())
                        || m.aircraftType().toString().toLowerCase().contains(searchText.toLowerCase()))
                .forEach(m -> items.add(new ModelRow(m)));
        modelsTable.setItems(items);
    }

    @FXML
    private void addAircraftModel() {
        if (!FieldValidator.isFormValid(newCodeError, newNameError, newManufacturerError,
                newTypeError, newMaxPassengersError, newEmptyWeightError,
                newMtowError, newMzfwError, newFuelCapacityError,
                newCeilingError, newCruiseSpeedError, newMaxRangeError,
                newWingAreaError, newCdError, newClError)) {
            NotificationManager.error("Validation Error", "Fix the highlighted fields before submitting.");
            return;
        }
        try {
            ctrl.createAircraftModel(
                    newCode.getText(),
                    newName.getText().isBlank() ? newCode.getText() : newName.getText(),
                    newManufacturer.getValue(),
                    newType.getValue(),
                    parseIntSafe(newMaxPassengers.getText()),
                    parseDoubleSafe(newEmptyWeight.getText()),
                    parseDoubleSafe(newMtow.getText()),
                    parseDoubleSafe(newMzfw.getText()),
                    parseDoubleSafe(newFuelCapacity.getText()),
                    parseDoubleSafe(newCeiling.getText()),
                    parseDoubleSafe(newCruiseSpeed.getText()),
                    parseDoubleSafe(newMaxRange.getText()),
                    parseDoubleSafe(newWingArea.getText()),
                    parseDoubleSafe(newCd.getText()),
                    parseDoubleSafe(newCl.getText())
            );
            NotificationManager.success("Model Created", "Aircraft model created successfully!");
            clearForm();
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    private void clearForm() {
        newCode.clear();
        newName.clear();
        newMaxPassengers.clear();
        newMtow.clear();
        newMzfw.clear();
        newEmptyWeight.clear();
        newFuelCapacity.clear();
        newCruiseSpeed.clear();
        newCeiling.clear();
        newMaxRange.clear();
        newWingArea.clear();
        newCd.clear();
        newCl.clear();
    }

    private Integer parseIntSafe(final String val) {
        if (val == null || val.isBlank()) return null;
        try { return Integer.parseInt(val); } catch (final NumberFormatException e) { return null; }
    }

    private double parseDoubleSafe(final String val) {
        if (val == null || val.isBlank()) return 0.0;
        try { return Double.parseDouble(val); } catch (final NumberFormatException e) { return 0.0; }
    }

    @FXML
    private void onZoom() {
        TableZoomUtil.openZoom(modelsTable, "Aircraft Models");
    }

    public static class ModelRow {
        public final SimpleStringProperty code;
        public final SimpleStringProperty name;
        public final SimpleStringProperty manufacturer;
        public final SimpleStringProperty type;
        public final SimpleStringProperty passengers;
        public final SimpleStringProperty engines;

        public ModelRow(final AircraftModel m) {
            code = new SimpleStringProperty(m.identity().toString());
            name = new SimpleStringProperty(m.name());
            manufacturer = new SimpleStringProperty(m.manufacturerName());
            type = new SimpleStringProperty(m.aircraftType().toString());
            passengers = new SimpleStringProperty(m.maxPassengers() != null ? String.valueOf(m.maxPassengers()) : "");
            engines = new SimpleStringProperty(m.variants().size() + " engine(s)");
        }
    }
}
