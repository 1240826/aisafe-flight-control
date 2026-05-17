package aisafe.lprog.visitor;

import aisafe.lprog.FlightPlanBaseVisitor;
import aisafe.lprog.FlightPlanParser;

/**
 * Visitor pattern (LPROG slides):
 *   - extends FlightPlanBaseVisitor<String>
 *   - each visitXxx returns a String fragment
 *   - children must be visited explicitly via visit(child)
 */
public class FlightPlanPrinterVisitor extends FlightPlanBaseVisitor<String> {

    @Override
    public String visitFlightFile(FlightPlanParser.FlightFileContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Flight Plan Summary ===\n");
        for (var f : ctx.flightDecl()) sb.append(visit(f)).append("\n");
        return sb.toString();
    }

    @Override
    public String visitFlightDecl(FlightPlanParser.FlightDeclContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Flight %-10s [%s]\n", visit(ctx.flightId()), visit(ctx.flightType())));
        sb.append(visit(ctx.routeDecl()));
        for (var leg : ctx.legDecl()) sb.append(visit(leg));
        return sb.toString();
    }

    @Override
    public String visitFlightId(FlightPlanParser.FlightIdContext ctx) {
        return ctx.IDENTIFIER().getText();
    }

    @Override
    public String visitFlightType(FlightPlanParser.FlightTypeContext ctx) {
        return ctx.REGULAR() != null ? "REGULAR" : "CHARTER";
    }

    @Override
    public String visitRouteDecl(FlightPlanParser.RouteDeclContext ctx) {
        return String.format("  Route    : %s -> %s\n",
                visit(ctx.airportCode(0)), visit(ctx.airportCode(1)));
    }

    @Override
    public String visitAirportCode(FlightPlanParser.AirportCodeContext ctx) {
        return ctx.IATA_CODE() != null ? ctx.IATA_CODE().getText() : ctx.ICAO_CODE().getText();
    }

    @Override
    public String visitLegDecl(FlightPlanParser.LegDeclContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  Leg %-6s\n", ctx.IDENTIFIER().getText()));
        sb.append(visit(ctx.departureDecl()));
        sb.append(visit(ctx.arrivalDecl()));
        sb.append(visit(ctx.fuelDecl()));
        for (var seg : ctx.segmentDecl()) sb.append(visit(seg));
        return sb.toString();
    }

    @Override
    public String visitDepartureDecl(FlightPlanParser.DepartureDeclContext ctx) {
        return String.format("    Departure : %s  %s  %s\n",
                visit(ctx.airportCode()),
                visit(ctx.scheduleField()),
                ctx.TIME_LITERAL().getText());
    }

    @Override
    public String visitScheduleField(FlightPlanParser.ScheduleFieldContext ctx) {
        return ctx.DATE_LITERAL() != null
                ? ctx.DATE_LITERAL().getText()
                : ctx.DAY_LITERAL().getText();
    }

    @Override
    public String visitArrivalDecl(FlightPlanParser.ArrivalDeclContext ctx) {
        return String.format("    Arrival   : %s  %s\n",
                visit(ctx.airportCode()),
                ctx.TIME_LITERAL().getText());
    }

    @Override
    public String visitFuelDecl(FlightPlanParser.FuelDeclContext ctx) {
        return String.format("    Fuel      : %s\n", ctx.numericValue().getText());
    }

    @Override
    public String visitSegmentDecl(FlightPlanParser.SegmentDeclContext ctx) {
        return String.format("    Segment %-5s from:%s to:%s alt:%s wind:%s\n",
                ctx.IDENTIFIER().getText(),
                visit(ctx.coordinatePair(0)),
                visit(ctx.coordinatePair(1)),
                visit(ctx.altitudeSlotList()),
                visit(ctx.windDecl()));
    }

    @Override
    public String visitCoordinatePair(FlightPlanParser.CoordinatePairContext ctx) {
        return String.format("(%s,%s)",
                ctx.numericValue(0).getText(), ctx.numericValue(1).getText());
    }

    @Override
    public String visitAltitudeSlotList(FlightPlanParser.AltitudeSlotListContext ctx) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ctx.altitudeSlot().size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(visit(ctx.altitudeSlot(i)));
        }
        return sb.append("]").toString();
    }

    @Override
    public String visitAltitudeSlot(FlightPlanParser.AltitudeSlotContext ctx) {
        String alt = ctx.numericValue(0).getText();
        return ctx.WIDTH() != null ? alt + " width " + ctx.numericValue(1).getText() : alt;
    }

    @Override
    public String visitWindDecl(FlightPlanParser.WindDeclContext ctx) {
        return String.format("(%s,%s)",
                ctx.numericValue(0).getText(), ctx.numericValue(1).getText());
    }
}
