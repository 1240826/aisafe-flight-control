package eapli.aisafe.flightplan.application;

import aisafe.lprog.FlightPlanLexer;
import aisafe.lprog.FlightPlanParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import java.util.List;
import java.util.Map;

public class FlightPlanToScenarioConverter {

    private static final Map<String, Integer> AIRPORT_ELEVATIONS = Map.ofEntries(
            Map.entry("LIS", 113), Map.entry("OPO", 51), Map.entry("CDG", 119),
            Map.entry("FRA", 111), Map.entry("LHR", 25), Map.entry("AMS", -3),
            Map.entry("FNC", 58), Map.entry("PDL", 79), Map.entry("TER", 55),
            Map.entry("FAO", 6), Map.entry("MAD", 609), Map.entry("BCN", 5),
            Map.entry("ALC", 43), Map.entry("AGP", 16), Map.entry("GRO", 11),
            Map.entry("IBZ", 6), Map.entry("MAH", 92), Map.entry("PMI", 7),
            Map.entry("SCQ", 120), Map.entry("VGO", 261), Map.entry("BIO", 42),
            Map.entry("SVQ", 34), Map.entry("TFN", 632), Map.entry("TFS", 64),
            Map.entry("LPA", 24), Map.entry("ACE", 14), Map.entry("WAW", 110)
    );

    public String convert(final String dslContent) {
        try {
            final CharStream input = CharStreams.fromString(dslContent);
            final FlightPlanLexer lexer = new FlightPlanLexer(input);
            final CommonTokenStream tokens = new CommonTokenStream(lexer);
            final FlightPlanParser parser = new FlightPlanParser(tokens);
            parser.removeErrorListeners();
            final var ctx = parser.flightFile();
            return visitFlightFile(ctx);
        } catch (final Exception e) {
            throw new IllegalArgumentException("DSL content is not a valid Flight DSL: " + e.getMessage(), e);
        }
    }

    public boolean canConvert(final String dslContent) {
        try {
            convert(dslContent);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    private String visitFlightFile(final FlightPlanParser.FlightFileContext ctx) {
        final var sb = new StringBuilder();
        sb.append("[\n");
        sb.append(visitFlightDecl(ctx.flightDecl()));
        sb.append("]\n");
        return sb.toString();
    }

    private String visitFlightDecl(final FlightPlanParser.FlightDeclContext ctx) {
        final var flightId = ctx.flightId().IDENTIFIER().getText();
        final var flightType = ctx.flightType().REGULAR() != null ? "regular" : "charter";
        final var routeOrigin = ctx.routeDecl().airportCode(0).getText();
        final var routeDestination = ctx.routeDecl().airportCode(1).getText();

        final var legs = ctx.legDecl();
        final var firstLeg = legs.get(0);
        final var departureTime = extractDepartureTime(firstLeg);
        final var departureTz = extractDepartureTz(firstLeg);

        final var sb = new StringBuilder();
        sb.append("  {\n");
        sb.append("    \"ID\": \"").append(flightId).append("\",\n");
        sb.append("    \"Type\": \"").append(flightType).append("\",\n");
        sb.append("    \"Route\": \"").append(routeOrigin).append("-").append(routeDestination).append("\",\n");
        sb.append("    \"DepartureTime\": \"").append(departureTime).append("\",\n");
        sb.append("    \"DepartureTZ\": \"").append(departureTz).append("\",\n");
        sb.append("    \"Leg\": [\n");

        for (int i = 0; i < legs.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append(visitLegDecl(legs.get(i), i, legs.size()));
        }

        sb.append("\n    ]\n");
        sb.append("  }\n");
        return sb.toString();
    }

    private String visitLegDecl(final FlightPlanParser.LegDeclContext ctx,
                                 final int legIndex, final int totalLegs) {
        final var depAirport = ctx.departureDecl().airportCode().getText();
        final var arrAirport = ctx.arrivalDecl().airportCode().getText();
        final var depElev = AIRPORT_ELEVATIONS.getOrDefault(depAirport, 0);
        final var arrElev = AIRPORT_ELEVATIONS.getOrDefault(arrAirport, 0);

        final var fuelCtx = ctx.fuelDecl().numericValue();
        final double fuelQty = Double.parseDouble(fuelCtx.NUMBER().getText());
        final String fuelUnit = fuelCtx.unit() != null ? fuelCtx.unit().getText() : "kg";

        final var segments = ctx.segmentDecl();

        final var sb = new StringBuilder();
        sb.append("      {\n");
        sb.append("        \"Departure\": \"").append(depAirport).append("\",\n");
        sb.append("        \"Arrival\": \"").append(arrAirport).append("\",\n");
        sb.append("        \"Fuel\": { \"Quantity\": ").append((long) fuelQty).append(", \"Unit\": \"").append(fuelUnit).append("\" },\n");
        sb.append(visitFlightProfile(segments));
        sb.append(",\n");
        sb.append(visitSegments(segments, depElev, arrElev));
        sb.append("\n      }");
        return sb.toString();
    }

    private String extractDepartureTime(final FlightPlanParser.LegDeclContext leg) {
        final var schedule = leg.departureDecl().departureSchedule();
        if (!schedule.daySchedule().isEmpty()) {
            return schedule.daySchedule(0).TIMESTAMP().getText().substring(11, 16);
        }
        return schedule.TIMESTAMP().getText().substring(11, 16);
    }

    private String extractDepartureTz(final FlightPlanParser.LegDeclContext leg) {
        final var schedule = leg.departureDecl().departureSchedule();
        final String ts;
        if (!schedule.daySchedule().isEmpty()) {
            ts = schedule.daySchedule(0).TIMESTAMP().getText();
        } else {
            ts = schedule.TIMESTAMP().getText();
        }
        final int tzStart = ts.indexOf('+') > 10 ? ts.indexOf('+')
                : ts.indexOf('-', 10);
        if (tzStart < 0) return "+00:00";
        return ts.substring(tzStart);
    }

    private String visitFlightProfile(final List<FlightPlanParser.SegmentDeclContext> segments) {
        double maxAlt = 0;
        for (final var seg : segments) {
            final var altSlot = seg.altitudeSlotList().altitudeSlot(0);
            final double alt = Double.parseDouble(altSlot.numericValue(0).NUMBER().getText());
            if (alt > maxAlt) maxAlt = alt;
        }
        final int cruiseAlt = (int) maxAlt;

        final var sb = new StringBuilder();
        sb.append("        \"Flight Profile\": {\n");
        sb.append("          \"Climb\": [\n");
        sb.append("            { \"Altitude\": { \"Value\": 0, \"Unit\": \"m\" }, \"Speed\": { \"Value\": 200, \"Unit\": \"Knots\" }, \"RateClimb\": { \"Value\": 11.0, \"Unit\": \"m/s\" } }");
        if (cruiseAlt > 3000) {
            sb.append(",\n            { \"Altitude\": { \"Value\": 3000, \"Unit\": \"m\" }, \"Speed\": { \"Value\": 260, \"Unit\": \"Knots\" }, \"RateClimb\": { \"Value\": 9.0, \"Unit\": \"m/s\" } }");
        }
        sb.append(",\n            { \"Altitude\": { \"Value\": ").append(cruiseAlt)
                .append(", \"Unit\": \"m\" }, \"Speed\": { \"Value\": 320, \"Unit\": \"Knots\" }, \"RateClimb\": { \"Value\": 6.0, \"Unit\": \"m/s\" } }\n");
        sb.append("          ],\n");

        sb.append("          \"Descend\": [\n");
        sb.append("            { \"Altitude\": { \"Value\": 0, \"Unit\": \"m\" }, \"Speed\": { \"Value\": 140, \"Unit\": \"Knots\" }, \"RateDescent\": { \"Value\": -5.0, \"Unit\": \"m/s\" } }");
        if (cruiseAlt > 3000) {
            sb.append(",\n            { \"Altitude\": { \"Value\": 3000, \"Unit\": \"m\" }, \"Speed\": { \"Value\": 260, \"Unit\": \"Knots\" }, \"RateDescent\": { \"Value\": -8.0, \"Unit\": \"m/s\" } }");
        }
        sb.append(",\n            { \"Altitude\": { \"Value\": ").append(cruiseAlt)
                .append(", \"Unit\": \"m\" }, \"Speed\": { \"Value\": 320, \"Unit\": \"Knots\" }, \"RateDescent\": { \"Value\": -10.0, \"Unit\": \"m/s\" } }\n");
        sb.append("          ],\n");

        sb.append("          \"Cruise\": { \"Speed\": { \"Value\": 440, \"Unit\": \"Knots\" } }\n");
        sb.append("        }");
        return sb.toString();
    }

    private String visitSegments(final List<FlightPlanParser.SegmentDeclContext> segments,
                                  final int depElev, final int arrElev) {
        final var sb = new StringBuilder();
        sb.append("        \"Segments\": [\n");
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append(visitSegment(segments.get(i), i, segments, depElev, arrElev));
        }
        sb.append("\n        ]");
        return sb.toString();
    }

    private String visitSegment(final FlightPlanParser.SegmentDeclContext ctx,
                                 final int index,
                                 final List<FlightPlanParser.SegmentDeclContext> allSegments,
                                 final int depElev, final int arrElev) {
        final var fromLat = Double.parseDouble(ctx.coordinatePair(0).numericValue(0).NUMBER().getText());
        final var fromLon = Double.parseDouble(ctx.coordinatePair(0).numericValue(1).NUMBER().getText());
        final var toLat = Double.parseDouble(ctx.coordinatePair(1).numericValue(0).NUMBER().getText());
        final var toLon = Double.parseDouble(ctx.coordinatePair(1).numericValue(1).NUMBER().getText());

        final var altSlot = ctx.altitudeSlotList().altitudeSlot(0);
        final double alt = Double.parseDouble(altSlot.numericValue(0).NUMBER().getText());

        final String mode;
        if (allSegments.size() > 1 && index == 0) {
            mode = "climb";
        } else if (allSegments.size() > 1 && index == allSegments.size() - 1) {
            mode = "descend";
        } else {
            mode = "cruise";
        }

        final int startAlt;
        final int endAlt;
        if (allSegments.size() == 1) {
            startAlt = (int) alt;
            endAlt = (int) alt;
        } else if (index == 0) {
            startAlt = depElev;
            endAlt = (int) alt;
        } else if (index == allSegments.size() - 1) {
            startAlt = (int) alt;
            endAlt = arrElev;
        } else {
            startAlt = (int) alt;
            endAlt = (int) alt;
        }

        final var sb = new StringBuilder();
        sb.append("          {\n");
        sb.append("            \"Mode\": \"").append(mode).append("\",\n");
        sb.append("            \"Start\": { \"Latitude\": ").append(fromLat)
                .append(", \"Longitude\": ").append(fromLon)
                .append(", \"Altitude\": { \"Quantity\": ").append(startAlt)
                .append(", \"Unit\": \"m\" } },\n");
        sb.append("            \"End\": { \"Latitude\": ").append(toLat)
                .append(", \"Longitude\": ").append(toLon)
                .append(", \"Altitude\": { \"Quantity\": ").append(endAlt)
                .append(", \"Unit\": \"m\" } }\n");
        sb.append("          }");
        return sb.toString();
    }
}
