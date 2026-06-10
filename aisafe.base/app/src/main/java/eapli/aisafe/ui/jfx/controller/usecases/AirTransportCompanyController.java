package eapli.aisafe.ui.jfx.controller.usecases;

import eapli.aisafe.company.application.RegisterAirTransportCompanyController;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.ui.jfx.util.NotificationManager;
import eapli.aisafe.ui.jfx.util.TableZoomUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.stream.StreamSupport;

public class AirTransportCompanyController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<CompanyRow> companiesTable;

    @FXML
    private TableColumn<CompanyRow, String> colName;

    @FXML
    private TableColumn<CompanyRow, String> colIata;

    @FXML
    private TableColumn<CompanyRow, String> colIcao;

    @FXML
    private TextField newIata;

    @FXML
    private TextField newIcao;

    @FXML
    private TextField newName;

    private final RegisterAirTransportCompanyController ctrl = new RegisterAirTransportCompanyController();
    private final ObservableList<CompanyRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colName.setCellValueFactory(d -> d.getValue().name);
        colIata.setCellValueFactory(d -> d.getValue().iata);
        colIcao.setCellValueFactory(d -> d.getValue().icao);
        refreshTable();
    }

    @FXML
    private void refreshTable() {
        items.clear();
        StreamSupport.stream(ctrl.allCompanies().spliterator(), false)
                .forEach(c -> items.add(new CompanyRow(
                        c.name(),
                        c.iata().toString(),
                        c.icao().toString()
                )));
        companiesTable.setItems(items);
    }

    @FXML
    private void addCompany() {
        try {
            if (newIata.getText().isBlank() || newIcao.getText().isBlank() || newName.getText().isBlank()) {
                NotificationManager.error("Validation Error", "IATA, ICAO, and Name are required.");
                return;
            }
            ctrl.registerCompany(
                    newIata.getText(),
                    newIcao.getText(),
                    newName.getText()
            );
            NotificationManager.success("Company Registered", "Company registered successfully!");
            newIata.clear();
            newIcao.clear();
            newName.clear();
            refreshTable();
        } catch (final Exception e) {
            NotificationManager.error("Error", e.getMessage());
        }
    }

    @FXML
    private void onZoom() {
        TableZoomUtil.openZoom(companiesTable, "Air Transport Companies");
    }

    public static class CompanyRow {
        public final SimpleStringProperty name;
        public final SimpleStringProperty iata;
        public final SimpleStringProperty icao;

        public CompanyRow(final String n, final String i, final String ic) {
            name = new SimpleStringProperty(n);
            iata = new SimpleStringProperty(i);
            icao = new SimpleStringProperty(ic);
        }
    }
}
