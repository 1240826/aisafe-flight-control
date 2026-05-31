package aisafe.lprog.listener;

import aisafe.lprog.FlightPlanBaseListener;
import aisafe.lprog.FlightPlanParser;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Listener pattern (LPROG slides):
 *   - extends FlightPlanBaseListener
 *   - called automatically by ParseTreeWalker — no explicit visit() calls
 *   - enterXxx fires on enter, exitXxx on leave
 *   - state accumulated across events for cross-block rules
 *
 * Semantic rules R2–R11 (specification + extensions):
 *   R2  — fuel quantity > 0
 *   R3  — arrival airport of leg N == departure airport of leg N+1
 *   R4  — arrival datetime of leg N < departure datetime of leg N+1 (always UTC-aware)
 *   R5  — route origin == first leg departure airport
 *   R6  — route destination == last leg arrival airport
 *   R7  — no airport visited more than once in the same flight
 *   R8  — segment from-coordinate != to-coordinate
 *   R9  — altitude and width positive
 *   R10 — timestamp is a valid ISO 8601 datetime with timezone
 *   R11 — charter departures use datetime: only; regular departures use day: + datetime:
 *         arrivals always use datetime: (destination timezone) — no R11 for arrivals
 *
 * Note: R1 (duplicate flight ID) is now enforced at the grammar level
 * via flightFile : flightDecl EOF — only one flight declaration per file is allowed.
 *
 * Extension fields (US080):
 *   aircraft — registration identifier of the aircraft (grammar-enforced IDENTIFIER format)
 *   pilot    — identifier of the responsible pilot (application layer validates company match)
 */
public class SemanticValidationListener extends FlightPlanBaseListener {

    private final List<String> errors = new ArrayList<>();

    // Per-flight state
    private String currentFlightType;  // "REGULAR" or "CHARTER" — tracked for R11
    private String currentAircraft;    // aircraft registration (US080 extension)
    private String currentPilot;       // responsible pilot identifier (US080 extension)
    private String routeOrigin;
    private String routeDestination;

    // Each leg: [depAirport, depTimestamp, arrAirport, arrTimestamp]
    // Both departure and arrival always carry full ISO 8601 timestamps with timezone.
    private final List<String[]> legs = new ArrayList<>();

    // Per-leg working state
    private String currentDepAirport;
    private String currentDepTimestamp;
    private String currentArrAirport;
    private String currentArrTimestamp;

    @Override
    public void enterFlightDecl(FlightPlanParser.FlightDeclContext ctx) {
        currentFlightType = ctx.flightType().REGULAR() != null ? "REGULAR" : "CHARTER";
        // IDENTIFIER(0) = aircraft registration, IDENTIFIER(1) = pilot id (US080 extension)
        currentAircraft = ctx.IDENTIFIER(0) != null ? ctx.IDENTIFIER(0).getText() : "";
        currentPilot    = ctx.IDENTIFIER(1) != null ? ctx.IDENTIFIER(1).getText() : "";
        routeOrigin = null;
        routeDestination = null;
        legs.clear();
    }

    // R3, R4, R5, R6, R7
    @Override
    public void exitFlightDecl(FlightPlanParser.FlightDeclContext ctx) {
        if (legs.isEmpty()) return;

        int line = ctx.getStart().getLine();
        String id = ctx.flightId().getText();

        // R5 — route origin must match first leg departure airport
        String firstDep = legs.get(0)[0];
        if (routeOrigin != null && !routeOrigin.equals(firstDep)) {
            error(line, "R5", "route origin '" + routeOrigin
                    + "' must match first leg departure airport '" + firstDep + "'");
        }

        // R6 — route destination must match last leg arrival airport
        String lastArr = legs.get(legs.size() - 1)[2];
        if (routeDestination != null && !routeDestination.equals(lastArr)) {
            error(line, "R6", "route destination '" + routeDestination
                    + "' must match last leg arrival airport '" + lastArr + "'");
        }

        // R3 — consecutive leg airport connection
        for (int i = 0; i < legs.size() - 1; i++) {
            String arr     = legs.get(i)[2];
            String nextDep = legs.get(i + 1)[0];
            if (!arr.equals(nextDep)) {
                error(line, "R3", "leg " + (i + 1) + " arrival airport '" + arr
                        + "' must match leg " + (i + 2) + " departure airport '" + nextDep + "'");
            }
        }

        // R4 — arrival must be before next departure; always UTC-aware (both types carry full timestamps)
        for (int i = 0; i < legs.size() - 1; i++) {
            String arrTs  = legs.get(i)[3];
            String nextTs = legs.get(i + 1)[1];
            try {
                OffsetDateTime arrDT     = OffsetDateTime.parse(arrTs);
                OffsetDateTime nextDepDT = OffsetDateTime.parse(nextTs);
                if (!arrDT.isBefore(nextDepDT)) {
                    error(line, "R4", "leg " + (i + 1) + " arrival (" + arrTs
                            + ") must be before leg " + (i + 2) + " departure (" + nextTs + ")");
                }
            } catch (DateTimeParseException e) {
                // R10 already reported the parse error for the invalid timestamp
            }
        }

        // R7 — no airport visited twice
        Set<String> visited = new LinkedHashSet<>();
        visited.add(legs.get(0)[0]);
        for (String[] leg : legs) {
            if (!visited.add(leg[2])) {
                error(line, "R7", "airport '" + leg[2]
                        + "' is visited more than once in flight '" + id + "'");
            }
        }
    }

    @Override
    public void exitRouteDecl(FlightPlanParser.RouteDeclContext ctx) {
        routeOrigin      = ctx.airportCode(0).getText();
        routeDestination = ctx.airportCode(1).getText();
    }

    // R10 (timestamp validity), R11 (departure schedule type matches flight type)
    @Override
    public void exitDepartureDecl(FlightPlanParser.DepartureDeclContext ctx) {
        currentDepAirport = ctx.airportCode().getText();
        var schedule = ctx.departureSchedule();

        // R11 — departure format must match flight type
        if (!schedule.daySchedule().isEmpty()) {
            // Regular format: day: + datetime: (one or more pairs)
            if ("CHARTER".equals(currentFlightType)) {
                error(schedule.getStart().getLine(), "R11",
                        "charter flights must specify departure as datetime: only, not day: + datetime:");
            }
            // Extract first timestamp for R4/R10; validate all
            currentDepTimestamp = schedule.daySchedule(0).TIMESTAMP().getText();
            for (int i = 0; i < schedule.daySchedule().size(); i++) {
                String ts = schedule.daySchedule(i).TIMESTAMP().getText();
                r10validate(ts, schedule.daySchedule(i).TIMESTAMP().getSymbol().getLine(), "departure");
            }
        } else {
            // Charter format: datetime: only
            if ("REGULAR".equals(currentFlightType)) {
                error(schedule.getStart().getLine(), "R11",
                        "regular flights must specify departure as day: + datetime:, not datetime: only");
            }
            String ts = schedule.TIMESTAMP().getText();
            currentDepTimestamp = ts;
            r10validate(ts, schedule.TIMESTAMP().getSymbol().getLine(), "departure");
        }
    }

    // R10 (arrival timestamp validity) — no R11 for arrivals (always datetime:)
    @Override
    public void exitArrivalDecl(FlightPlanParser.ArrivalDeclContext ctx) {
        currentArrAirport = ctx.airportCode().getText();
        String ts = ctx.arrivalSchedule().TIMESTAMP().getText();
        currentArrTimestamp = ts;
        r10validate(ts, ctx.arrivalSchedule().TIMESTAMP().getSymbol().getLine(), "arrival");
    }

    @Override
    public void exitLegDecl(FlightPlanParser.LegDeclContext ctx) {
        // Store per-leg data: both departure and arrival carry full ISO timestamps
        legs.add(new String[]{
            currentDepAirport, currentDepTimestamp,
            currentArrAirport, currentArrTimestamp
        });
    }

    // R2 — fuel quantity strictly positive
    @Override
    public void exitFuelDecl(FlightPlanParser.FuelDeclContext ctx) {
        double qty = number(ctx.numericValue());
        if (qty <= 0) {
            error(ctx.getStart().getLine(), "R2",
                    "fuel quantity must be positive, got: " + ctx.numericValue().getText());
        }
    }

    // R8 — segment from != to
    @Override
    public void exitSegmentDecl(FlightPlanParser.SegmentDeclContext ctx) {
        var fromCtx = ctx.coordinatePair(0);
        var toCtx   = ctx.coordinatePair(1);

        double fLat = number(fromCtx.numericValue(0)), fLon = number(fromCtx.numericValue(1));
        double tLat = number(toCtx.numericValue(0)),   tLon = number(toCtx.numericValue(1));

        if (Double.compare(fLat, tLat) == 0 && Double.compare(fLon, tLon) == 0) {
            int idx = ((FlightPlanParser.LegDeclContext) ctx.getParent()).segmentDecl().indexOf(ctx) + 1;
            error(ctx.getStart().getLine(), "R8",
                    "segment " + idx + ": from and to coordinates must differ");
        }
    }

    // R9 — altitude and corridor width must be positive
    @Override
    public void exitAltitudeSlot(FlightPlanParser.AltitudeSlotContext ctx) {
        double alt = number(ctx.numericValue(0));
        if (alt <= 0) {
            error(ctx.getStart().getLine(), "R9",
                    "altitude must be positive, got: " + ctx.numericValue(0).getText());
        }
        double w = number(ctx.numericValue(1));
        if (w <= 0) {
            error(ctx.getStart().getLine(), "R9",
                    "corridor width must be positive, got: " + ctx.numericValue(1).getText());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void r10validate(String ts, int line, String label) {
        try {
            OffsetDateTime.parse(ts);
        } catch (DateTimeParseException e) {
            error(line, "R10",
                    label + " timestamp '" + ts + "' is not a valid ISO 8601 datetime");
        }
    }

    private double number(FlightPlanParser.NumericValueContext ctx) {
        return Double.parseDouble(ctx.NUMBER().getText());
    }

    private void error(int line, String rule, String msg) {
        errors.add(String.format("[SEMANTIC] line %d - [%s] %s", line, rule, msg));
    }

    public boolean hasErrors()      { return !errors.isEmpty(); }
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }
    public void printErrors()       { errors.forEach(System.err::println); }
}
