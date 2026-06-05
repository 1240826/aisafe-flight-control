package eapli.aisafe.remote.pilot;

import eapli.aisafe.aircraft.application.ListCompanyFleetController;
import eapli.aisafe.flightplan.application.ImportFlightPlanController;
import eapli.aisafe.flightplan.application.TestFlightPlanController;
import eapli.aisafe.report.application.GenerateMonthlyReportController;
import eapli.aisafe.report.domain.MonthlyReport;
import eapli.aisafe.simulation.application.GenerateSimulationReportController;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RemotePilotService {

    private final ListCompanyFleetController fleetController;
    private final ImportFlightPlanController importController;
    private final TestFlightPlanController testController;
    private final GenerateSimulationReportController reportController;
    private final GenerateMonthlyReportController monthlyController;

    public RemotePilotService() {
        this.fleetController = new ListCompanyFleetController();
        this.importController = new ImportFlightPlanController();
        this.testController = new TestFlightPlanController();
        this.reportController = new GenerateSimulationReportController();
        this.monthlyController = new GenerateMonthlyReportController();
    }

    public List<AircraftDTO> listFleet() {
        return StreamSupport.stream(fleetController.allActiveAircraft().spliterator(), false)
                .map(AircraftDTO::from)
                .collect(Collectors.toList());
    }

    public ImportFlightPlanController.DslValidationResult createFlightPlan(
            final String flightId, final String dsl) {
        return importController.importFlightPlan(dsl, "remote-" + flightId, flightId);
    }

    public ImportFlightPlanController.DslValidationResult importFlightPlan(
            final String flightId, final String dsl) {
        return importController.importFlightPlan(dsl, "remote-" + flightId, flightId);
    }

    public TestFlightPlanController.TestResult validateFlightPlan(final String flightPlanId) {
        return testController.testFlightPlan(flightPlanId);
    }

    public String generateReport(final String areaCode) {
        return reportController.generateReport(areaCode);
    }

    public MonthlyReport monthlyReport(final int year, final int month) {
        return monthlyController.generateForMonth(YearMonth.of(year, month));
    }

    public List<?> listFlights() {
        return StreamSupport.stream(testController.allFlights().spliterator(), false)
                .collect(Collectors.toList());
    }

    public List<?> listRoutes() {
        return StreamSupport.stream(
                eapli.aisafe.infrastructure.persistence.PersistenceContext
                        .repositories().flightRoutes().findAll().spliterator(), false)
                .collect(Collectors.toList());
    }
}
