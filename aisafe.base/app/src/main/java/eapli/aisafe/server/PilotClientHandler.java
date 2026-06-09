package eapli.aisafe.server;

import eapli.aisafe.flightplan.application.ImportFlightPlanController;
import eapli.aisafe.flightplan.application.TestFlightPlanController;
import eapli.aisafe.remote.RemoteProtocol;
import eapli.aisafe.remote.pilot.AircraftDTO;
import eapli.aisafe.remote.pilot.RemotePilotService;
import eapli.aisafe.report.domain.MonthlyReport;
import eapli.aisafe.usermanagement.domain.AISafeRoles;

import java.net.Socket;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * US086 — handles one Pilot (FCO) TCP session.
 *
 * <p>Supported commands (after AUTH):
 * <ul>
 *   <li>{@code LIST_FLEET}</li>
 *   <li>{@code CREATE_FLIGHT_PLAN|flight_id|dsl_content}</li>
 *   <li>{@code IMPORT_FLIGHT_PLAN|flight_id|dsl_content}</li>
 *   <li>{@code VALIDATE_FLIGHT_PLAN|flight_plan_id}</li>
 *   <li>{@code GENERATE_REPORT|area_code}</li>
 *   <li>{@code MONTHLY_REPORT|year|month}</li>
 *   <li>{@code LIST_FLIGHTS}</li>
 *   <li>{@code LIST_ROUTES}</li>
 * </ul>
 */
class PilotClientHandler extends AbstractClientHandler {

    private final RemotePilotService pilotService;

    PilotClientHandler(final Socket clientSocket) {
        super(clientSocket, RemoteProtocol.SVC_PILOT, AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        this.pilotService = new RemotePilotService();
    }

    @Override
    protected String handleCommand(final String cmd, final String[] fields) {
        return switch (cmd) {
            case RemoteProtocol.CMD_LIST_FLEET           -> doListFleet();
            case RemoteProtocol.CMD_CREATE_FLIGHT_PLAN   -> doCreateFlightPlan(fields);
            case RemoteProtocol.CMD_IMPORT_FLIGHT_PLAN   -> doImportFlightPlan(fields);
            case RemoteProtocol.CMD_VALIDATE_FLIGHT_PLAN -> doValidateFlightPlan(fields);
            case RemoteProtocol.CMD_GENERATE_REPORT      -> doGenerateReport(fields);
            case RemoteProtocol.CMD_MONTHLY_REPORT       -> doMonthlyReport(fields);
            case RemoteProtocol.CMD_LIST_FLIGHTS         -> doListFlights();
            case RemoteProtocol.CMD_LIST_ROUTES          -> doListRoutes();
            default -> RemoteProtocol.err("Unknown command: " + cmd);
        };
    }

    // ── LIST_FLEET ────────────────────────────────────────────────────────────

    private String doListFleet() {
        try {
            final var aircraft = pilotService.listFleet();
            if (aircraft.isEmpty()) {
                return RemoteProtocol.ok("0 aircraft");
            }
            final StringBuilder sb = new StringBuilder();
            int count = 0;
            for (final AircraftDTO a : aircraft) {
                if (count > 0) sb.append(";");
                sb.append(a.registrationNumber()).append(",")
                        .append(a.aircraftModelCode()).append(",")
                        .append(a.operationalStatus()).append(",")
                        .append(a.totalCapacity());
                count++;
            }
            return RemoteProtocol.ok(count + " aircraft: " + sb);
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── CREATE_FLIGHT_PLAN|flight_id|dsl_content ─────────────────────────────

    private String doCreateFlightPlan(final String[] f) {
        if (f.length < 3) {
            return RemoteProtocol.err("Usage: CREATE_FLIGHT_PLAN|flight_id|dsl_content");
        }
        try {
            final String flightId = f[1];
            final String dsl      = f[2];
            final var result = pilotService.createFlightPlan(flightId, dsl);
            if (result.allPassed()) {
                return RemoteProtocol.ok("Flight plan created for " + flightId
                        + " | " + result.summary());
            }
            return RemoteProtocol.err("Validation failed: "
                    + String.join("; ", result.allErrors()));
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── IMPORT_FLIGHT_PLAN|flight_id|dsl_content ─────────────────────────────
    // Client sends DSL content directly (file reading happens client-side).

    private String doImportFlightPlan(final String[] f) {
        return doCreateFlightPlan(f);
    }

    // ── VALIDATE_FLIGHT_PLAN|flight_plan_id ──────────────────────────────────

    private String doValidateFlightPlan(final String[] f) {
        if (f.length < 2) {
            return RemoteProtocol.err("Usage: VALIDATE_FLIGHT_PLAN|flight_plan_id");
        }
        try {
            final String fpId = f[1];
            final var result = pilotService.validateFlightPlan(fpId);
            return RemoteProtocol.ok(result.message() == null ? "PASSED" : result.message());
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── GENERATE_REPORT|area_code ────────────────────────────────────────────

    private String doGenerateReport(final String[] f) {
        if (f.length < 2) {
            return RemoteProtocol.err("Usage: GENERATE_REPORT|area_code");
        }
        try {
            final String path = pilotService.generateReport(f[1]);
            return RemoteProtocol.ok("Report saved to " + path);
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── MONTHLY_REPORT|year|month ────────────────────────────────────────────

    private String doMonthlyReport(final String[] f) {
        if (f.length < 3) {
            return RemoteProtocol.err("Usage: MONTHLY_REPORT|year|month");
        }
        try {
            final int year  = Integer.parseInt(f[1]);
            final int month = Integer.parseInt(f[2]);
            final MonthlyReport report = pilotService.monthlyReport(year, month);
            return RemoteProtocol.ok(report.toString().replace("\r\n", "\n").replace("\n", "\\n"));
        } catch (final DateTimeParseException e) {
            return RemoteProtocol.err("Invalid date: " + e.getMessage());
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── LIST_FLIGHTS ──────────────────────────────────────────────────────────

    private String doListFlights() {
        try {
            final var flights = pilotService.listFlights();
            if (flights.isEmpty()) {
                return RemoteProtocol.ok("0 flights");
            }
            final StringBuilder sb = new StringBuilder();
            int count = 0;
            for (final Object obj : flights) {
                if (count > 0) sb.append(";");
                final var flight = (eapli.aisafe.flight.domain.Flight) obj;
                sb.append(flight.identity()).append(",")
                        .append(flight.departureTime()).append(",")
                        .append(flight.routeName());
                count++;
            }
            return RemoteProtocol.ok(count + " flights: " + sb);
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── LIST_ROUTES ───────────────────────────────────────────────────────────

    private String doListRoutes() {
        try {
            final var routes = pilotService.listRoutes();
            if (routes.isEmpty()) {
                return RemoteProtocol.ok("0 routes");
            }
            final StringBuilder sb = new StringBuilder();
            int count = 0;
            for (final Object obj : routes) {
                if (count > 0) sb.append(";");
                final var route = (eapli.aisafe.flightroute.domain.FlightRoute) obj;
                sb.append(route.identity()).append(",")
                        .append(route.origin()).append(",->,")
                        .append(route.destination());
                count++;
            }
            return RemoteProtocol.ok(count + " routes: " + sb);
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }
}
