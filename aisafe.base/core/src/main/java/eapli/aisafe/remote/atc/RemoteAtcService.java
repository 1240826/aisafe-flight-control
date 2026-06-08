package eapli.aisafe.remote.atc;

import eapli.aisafe.aircraft.application.AddAircraftController;
import eapli.aisafe.aircraft.application.DecommissionAircraftController;
import eapli.aisafe.aircraft.application.ListCompanyFleetController;
import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.flightroute.application.CreateFlightRouteController;
import eapli.aisafe.flightroute.application.DeleteFlightRouteController;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.pilot.application.AddPilotController;
import eapli.aisafe.pilot.application.ListPilotRosterController;
import eapli.aisafe.pilot.application.RemovePilotController;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RemoteAtcService {

    private final AddAircraftController addAircraftCtrl;
    private final DecommissionAircraftController decommAircraftCtrl;
    private final ListCompanyFleetController listFleetCtrl;
    private final CreateFlightRouteController createRouteCtrl;
    private final DeleteFlightRouteController deleteRouteCtrl;
    private final AddPilotController addPilotCtrl;
    private final ListPilotRosterController listPilotsCtrl;
    private final RemovePilotController removePilotCtrl;

    public RemoteAtcService() {
        this.addAircraftCtrl    = new AddAircraftController();
        this.decommAircraftCtrl = new DecommissionAircraftController();
        this.listFleetCtrl      = new ListCompanyFleetController();
        this.createRouteCtrl    = new CreateFlightRouteController();
        this.deleteRouteCtrl    = new DeleteFlightRouteController();
        this.addPilotCtrl       = new AddPilotController();
        this.listPilotsCtrl     = new ListPilotRosterController();
        this.removePilotCtrl    = new RemovePilotController();
    }

    // ── ADD_AIRCRAFT: regNumber|regCountry|modelCode|companyIata|crewMembers|date

    public String addAircraft(final String regNumber, final String regCountry,
                              final String modelCode, final String companyIata,
                              final int crewMembers, final String dateStr) {
        final LocalDate date = LocalDate.parse(dateStr);
        final List<SeatClass> seats = new ArrayList<>();
        seats.add(new SeatClass("Economy", 150));
        addAircraftCtrl.addAircraft(regNumber, regCountry, modelCode, companyIata,
                crewMembers, seats, date);
        return "Aircraft " + regNumber + " added";
    }

    // ── DECOMMISSION_AIRCRAFT: regNumber|regCountry

    public String decommissionAircraft(final String regNumber, final String regCountry) {
        decommAircraftCtrl.decommissionAircraft(regNumber, regCountry);
        return "Aircraft " + regNumber + " decommissioned";
    }

    // ── LIST_FLEET

    public String listFleet() {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        for (final Aircraft a : listFleetCtrl.allActiveAircraft()) {
            if (count > 0) sb.append(";");
            sb.append(a.identity()).append(",")
                    .append(a.aircraftModelCode()).append(",")
                    .append(a.operationalStatus()).append(",")
                    .append(a.totalCapacity());
            count++;
        }
        return count + " aircraft: " + sb;
    }

    public String listFleetByCompany(final String companyIata) {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        for (final Aircraft a : listFleetCtrl.fleetOfCompany(companyIata)) {
            if (count > 0) sb.append(";");
            sb.append(a.identity()).append(",")
                    .append(a.aircraftModelCode()).append(",")
                    .append(a.operationalStatus()).append(",")
                    .append(a.totalCapacity());
            count++;
        }
        return count + " aircraft: " + sb;
    }

    // ── CREATE_ROUTE: routeName|companyIata|originCode|destCode

    public String createRoute(final String routeName, final String companyIata,
                              final String originCode, final String destinationCode) {
        createRouteCtrl.createFlightRoute(routeName, companyIata, originCode, destinationCode);
        return "Route " + routeName + " created";
    }

    // ── DELETE_ROUTE: routeName|date (yyyy-mm-dd)

    public String deleteRoute(final String routeName, final String dateStr) {
        final LocalDate date = LocalDate.parse(dateStr);
        deleteRouteCtrl.deactivateRoute(routeName, date);
        return "Route " + routeName + " deactivated";
    }

    // ── ADD_PILOT: licenseNumber|companyIata|certDate|model1,model2,...

    public String addPilot(final String licenseNumber, final String companyIata,
                           final String certDateStr, final String modelsStr) {
        final LocalDate certDate = LocalDate.parse(certDateStr);
        final Set<String> modelCodes = new HashSet<>();
        for (final String m : modelsStr.split(",")) {
            modelCodes.add(m.trim());
        }
        addPilotCtrl.addPilot(licenseNumber,
                eapli.aisafe.company.domain.CompanyIATA.valueOf(companyIata),
                modelCodes.stream()
                        .map(eapli.aisafe.aircraftmodel.domain.AircraftModelCode::valueOf)
                        .collect(Collectors.toSet()),
                certDate);
        return "Pilot " + licenseNumber + " added";
    }

    // ── LIST_PILOTS: companyIata

    public String listPilots(final String companyIata) {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        for (final Pilot p : listPilotsCtrl.listCompanyPilots(
                eapli.aisafe.company.domain.CompanyIATA.valueOf(companyIata))) {
            if (count > 0) sb.append(";");
            sb.append(p.identity()).append(",").append(p.isActive() ? "ACTIVE" : "INACTIVE");
            count++;
        }
        return count + " pilots: " + sb;
    }

    // ── REMOVE_PILOT: licenseNumber

    public String removePilot(final String licenseNumber) {
        removePilotCtrl.deactivatePilot(PilotId.valueOf(licenseNumber));
        return "Pilot " + licenseNumber + " removed";
    }

    // ── LIST_ROUTES

    public String listRoutes() {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        for (final FlightRoute r : deleteRouteCtrl.activeRoutes()) {
            if (count > 0) sb.append(";");
            sb.append(r.identity()).append(",")
                    .append(r.origin()).append("->")
                    .append(r.destination());
            count++;
        }
        return count + " routes: " + sb;
    }
}