package aisafe.lprog.visitor;

import aisafe.lprog.FlightPlanBaseVisitor;
import aisafe.lprog.FlightPlanParser;

public class FlightPlanPrinterVisitor extends FlightPlanBaseVisitor<String> {

    @Override
    public String visitFlightFile(FlightPlanParser.FlightFileContext ctx) {
        return "=== Flight Plan Summary ===\n" + visit(ctx.flightDecl()) + "\n";
    }

    @Override
    public String visitFlightDecl(FlightPlanParser.FlightDeclContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Flight %-10s [%s]\n", visit(ctx.flightId()), visit(ctx.flightType())));
        sb.append(String.format("  Aircraft : %s\n", ctx.IDENTIFIER(0).getText()));
        sb.append(String.format("  Pilot    : %s\n", ctx.IDENTIFIER(1).getText()));
        sb.append(visit(ctx.routeDecl()));
        for (var leg : ctx.legDecl()) sb.append(visit(leg));
        return sb.toString();
    }

    @Override
    public String visitFlightId(FlightPlanParser.FlightIdContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            return ctx.IDENTIFIER().getText();
        }
        return ctx.NUMBER().getText();
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
        int idx = ((FlightPlanParser.FlightDeclContext) ctx.getParent()).legDecl().indexOf(ctx) + 1;
        sb.append(String.format("  Leg %d\n", idx));
        sb.append(visit(ctx.departureDecl()));
        sb.append(visit(ctx.arrivalDecl()));
        sb.append(visit(ctx.fuelDecl()));
        for (var seg : ctx.segmentDecl()) sb.append(visit(seg));
        return sb.toString();
    }

    @Override
    public String visitDepartureDecl(FlightPlanParser.DepartureDeclContext ctx) {
        return String.format("    Departure : %s  %s\n",
                visit(ctx.airportCode()),
                visit(ctx.departureSchedule()));
    }

    @Override
    public String visitDepartureSchedule(FlightPlanParser.DepartureScheduleContext ctx) {
        if (!ctx.daySchedule().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (var ds : ctx.daySchedule()) sb.append(visit(ds)).append("  ");
            return sb.toString().trim();
        } else {
            return ctx.TIMESTAMP().getText();
        }
    }

    @Override
    public String visitDaySchedule(FlightPlanParser.DayScheduleContext ctx) {
        return ctx.DAY_OF_WEEK().getText() + "  " + ctx.TIMESTAMP().getText();
    }

    @Override
    public String visitArrivalDecl(FlightPlanParser.ArrivalDeclContext ctx) {
        return String.format("    Arrival   : %s  %s\n",
                visit(ctx.airportCode()),
                visit(ctx.arrivalSchedule()));
    }

    @Override
    public String visitArrivalSchedule(FlightPlanParser.ArrivalScheduleContext ctx) {
        return ctx.TIMESTAMP().getText();
    }

    @Override
    public String visitFuelDecl(FlightPlanParser.FuelDeclContext ctx) {
        return String.format("    Fuel      : %s\n", ctx.numericValue().getText());
    }

    @Override
    public String visitSegmentDecl(FlightPlanParser.SegmentDeclContext ctx) {
        int idx = ((FlightPlanParser.LegDeclContext) ctx.getParent()).segmentDecl().indexOf(ctx) + 1;
        return String.format("    Segment %d from:%s to:%s alt:%s\n",
                idx,
                visit(ctx.coordinatePair(0)),
                visit(ctx.coordinatePair(1)),
                visit(ctx.altitudeSlotList()));
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
        return ctx.numericValue(0).getText() + " width " + ctx.numericValue(1).getText();
    }
}
