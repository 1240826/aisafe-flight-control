package aisafe.lprog.listener;

import aisafe.lprog.FlightPlanBaseListener;
import aisafe.lprog.FlightPlanParser;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Listener pattern (LPROG slides):
 *   - extends FlightPlanBaseListener
 *   - called automatically by ParseTreeWalker — no explicit visit() calls
 *   - enterXxx fires on enter, exitXxx on leave
 *   - state accumulated across events for cross-block rules
 *
 * Semantic rules R1–R11 (specification §3.4.5 + extension):
 *   R1  — flight identifier unique within file (symbol table)
 *   R2  — fuel quantity > 0
 *   R3  — arrival airport of leg N == departure airport of leg N+1
 *   R4  — arrival datetime of leg N < departure datetime of leg N+1
 *   R5  — route origin == first leg departure airport
 *   R6  — route destination == last leg arrival airport
 *   R7  — no airport visited more than once in the same flight
 *   R8  — segment from-coordinate != to-coordinate
 *   R9  — altitude, width, wind speed positive; direction 0-360
 *   R10 — date and time are valid calendar values
 *   R11 — regular flights must use day-of-week (day:); charter flights must use a date (date:)
 */
public class SemanticValidationListener extends FlightPlanBaseListener {

    private final List<String> errors = new ArrayList<>();

    // Symbol table — R1
    private final Set<String> seenFlightIds = new HashSet<>();

    // Per-flight state
    private String currentFlightType;    // "REGULAR" or "CHARTER" — R11
    private String routeOrigin;
    private String routeDestination;
    private final List<String[]> legs = new ArrayList<>(); // [depAirport, depDateOrDay, depTime, arrAirport, arrTime]

    // Per-leg working state
    private String currentDepAirport;
    private String currentDepDateOrDay;  // ISO date for charter, day name for regular
    private String currentDepTime;
    private String currentArrAirport;
    private String currentArrTime;

    // R1 + reset per-flight state
    @Override
    public void enterFlightDecl(FlightPlanParser.FlightDeclContext ctx) {
        String id = ctx.flightId().getText();
        if (!seenFlightIds.add(id)) {
            error(ctx.getStart().getLine(), "R1",
                    "flight identifier '" + id + "' already declared in this file");
        }
        currentFlightType = ctx.flightType().REGULAR() != null ? "REGULAR" : "CHARTER";
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

        // R5
        String firstDep = legs.get(0)[0];
        if (routeOrigin != null && !routeOrigin.equals(firstDep)) {
            error(line, "R5", "route origin '" + routeOrigin
                    + "' must match first leg departure airport '" + firstDep + "'");
        }

        // R6
        String lastArr = legs.get(legs.size() - 1)[3];
        if (routeDestination != null && !routeDestination.equals(lastArr)) {
            error(line, "R6", "route destination '" + routeDestination
                    + "' must match last leg arrival airport '" + lastArr + "'");
        }

        // R3
        for (int i = 0; i < legs.size() - 1; i++) {
            String arr     = legs.get(i)[3];
            String nextDep = legs.get(i + 1)[0];
            if (!arr.equals(nextDep)) {
                error(line, "R3", "leg " + (i + 1) + " arrival airport '" + arr
                        + "' must match leg " + (i + 2) + " departure airport '" + nextDep + "'");
            }
        }

        // R4 — cross-leg time ordering; comparison method depends on schedule type
        for (int i = 0; i < legs.size() - 1; i++) {
            String depRef  = legs.get(i)[1];
            String arrTime = legs.get(i)[4];
            String nextRef = legs.get(i + 1)[1];
            String nextTime = legs.get(i + 1)[2];

            boolean ordered;
            if (depRef.contains("-")) {
                // Charter — ISO 8601 lexicographic comparison equals chronological comparison
                ordered = (depRef + "T" + pad(arrTime)).compareTo(nextRef + "T" + pad(nextTime)) < 0;
            } else {
                // Regular — compare day ordinal * 1440 + time in minutes
                int arrScore  = dayOrdinal(depRef)  * 1440 + timeToMinutes(pad(arrTime));
                int depScore  = dayOrdinal(nextRef)  * 1440 + timeToMinutes(pad(nextTime));
                ordered = arrScore < depScore;
            }

            if (!ordered) {
                error(line, "R4", "leg " + (i + 1) + " arrival (" + depRef
                        + " " + arrTime + ") must be before leg " + (i + 2)
                        + " departure (" + nextRef + " " + nextTime + ")");
            }
        }

        // R7
        Set<String> visited = new LinkedHashSet<>();
        visited.add(legs.get(0)[0]);
        for (String[] leg : legs) {
            if (!visited.add(leg[3])) {
                error(line, "R7", "airport '" + leg[3]
                        + "' is visited more than once in flight '" + id + "'");
            }
        }
    }

    @Override
    public void exitRouteDecl(FlightPlanParser.RouteDeclContext ctx) {
        routeOrigin      = ctx.airportCode(0).getText();
        routeDestination = ctx.airportCode(1).getText();
    }

    // R10 on departure date/day, R11 on schedule type consistency
    @Override
    public void exitDepartureDecl(FlightPlanParser.DepartureDeclContext ctx) {
        currentDepAirport = ctx.airportCode().getText();
        currentDepTime    = ctx.TIME_LITERAL().getText();
        validateTime(currentDepTime, ctx.TIME_LITERAL().getSymbol().getLine(), "departure time");

        var schedule = ctx.scheduleField();

        if (schedule.DATE_LITERAL() != null) {
            // Charter-style: specific date
            currentDepDateOrDay = schedule.DATE_LITERAL().getText();
            try {
                LocalDate.parse(currentDepDateOrDay);
            } catch (DateTimeParseException e) {
                error(schedule.DATE_LITERAL().getSymbol().getLine(), "R10",
                        "departure date '" + currentDepDateOrDay + "' is not a valid calendar date");
            }
            // R11
            if ("REGULAR".equals(currentFlightType)) {
                error(schedule.getStart().getLine(), "R11",
                        "regular flights must specify a day of week (day:), not a specific date (date:)");
            }
        } else {
            // Regular-style: day of week
            currentDepDateOrDay = schedule.DAY_LITERAL().getText();
            // R11
            if ("CHARTER".equals(currentFlightType)) {
                error(schedule.getStart().getLine(), "R11",
                        "charter flights must specify a specific date (date:), not a day of week (day:)");
            }
        }
    }

    // R10 on arrival time
    @Override
    public void exitArrivalDecl(FlightPlanParser.ArrivalDeclContext ctx) {
        currentArrAirport = ctx.airportCode().getText();
        currentArrTime    = ctx.TIME_LITERAL().getText();
        validateTime(currentArrTime, ctx.TIME_LITERAL().getSymbol().getLine(), "arrival time");
    }

    // accumulate leg data
    @Override
    public void exitLegDecl(FlightPlanParser.LegDeclContext ctx) {
        legs.add(new String[]{
            currentDepAirport, currentDepDateOrDay, currentDepTime,
            currentArrAirport, currentArrTime
        });
    }

    // R2
    @Override
    public void exitFuelDecl(FlightPlanParser.FuelDeclContext ctx) {
        double qty = number(ctx.numericValue());
        if (qty <= 0) {
            error(ctx.getStart().getLine(), "R2",
                    "fuel quantity must be positive, got: " + ctx.numericValue().getText());
        }
    }

    // R8
    @Override
    public void exitSegmentDecl(FlightPlanParser.SegmentDeclContext ctx) {
        String id   = ctx.IDENTIFIER().getText();
        var fromCtx = ctx.coordinatePair(0);
        var toCtx   = ctx.coordinatePair(1);

        double fLat = number(fromCtx.numericValue(0)), fLon = number(fromCtx.numericValue(1));
        double tLat = number(toCtx.numericValue(0)),   tLon = number(toCtx.numericValue(1));

        if (Double.compare(fLat, tLat) == 0 && Double.compare(fLon, tLon) == 0) {
            error(ctx.getStart().getLine(), "R8",
                    "segment '" + id + "': from and to coordinates must differ");
        }
    }

    // R9: altitude and width > 0
    @Override
    public void exitAltitudeSlot(FlightPlanParser.AltitudeSlotContext ctx) {
        double alt = number(ctx.numericValue(0));
        if (alt <= 0) {
            error(ctx.getStart().getLine(), "R9",
                    "altitude must be positive, got: " + ctx.numericValue(0).getText());
        }
        if (ctx.WIDTH() != null && ctx.numericValue().size() > 1) {
            double w = number(ctx.numericValue(1));
            if (w <= 0) {
                error(ctx.getStart().getLine(), "R9",
                        "corridor width must be positive, got: " + ctx.numericValue(1).getText());
            }
        }
    }

    // R9: direction 0-360, speed >= 0
    @Override
    public void exitWindDecl(FlightPlanParser.WindDeclContext ctx) {
        double dir   = number(ctx.numericValue(0));
        double speed = number(ctx.numericValue(1));
        if (dir < 0 || dir > 360) {
            error(ctx.getStart().getLine(), "R9",
                    "wind direction must be in [0, 360], got: " + ctx.numericValue(0).getText());
        }
        if (speed < 0) {
            error(ctx.getStart().getLine(), "R9",
                    "wind speed must be non-negative, got: " + ctx.numericValue(1).getText());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private double number(FlightPlanParser.NumericValueContext ctx) {
        return Double.parseDouble(ctx.NUMBER().getText());
    }

    private void validateTime(String t, int line, String label) {
        String[] p = t.split(":");
        try {
            int hh = Integer.parseInt(p[0]);
            int mm = Integer.parseInt(p[1]);
            int ss = p.length > 2 ? Integer.parseInt(p[2]) : 0;
            if (hh > 23 || mm > 59 || ss > 59 || hh < 0 || mm < 0 || ss < 0) {
                error(line, "R10", label + " '" + t + "' is not a valid time value");
            }
        } catch (NumberFormatException e) {
            error(line, "R10", label + " '" + t + "' cannot be parsed");
        }
    }

    private String pad(String t) {
        return t.length() == 5 ? t + ":00" : t;
    }

    private int timeToMinutes(String t) {
        String[] p = t.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    private int dayOrdinal(String dayName) {
        return switch (dayName.toLowerCase()) {
            case "monday"    -> 1;
            case "tuesday"   -> 2;
            case "wednesday" -> 3;
            case "thursday"  -> 4;
            case "friday"    -> 5;
            case "saturday"  -> 6;
            case "sunday"    -> 7;
            default          -> 0;
        };
    }

    private void error(int line, String rule, String msg) {
        errors.add(String.format("[SEMANTIC] line %d - [%s] %s", line, rule, msg));
    }

    public boolean hasErrors()      { return !errors.isEmpty(); }
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }
    public void printErrors()       { errors.forEach(System.err::println); }
}
