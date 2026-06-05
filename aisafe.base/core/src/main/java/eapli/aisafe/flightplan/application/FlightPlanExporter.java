package eapli.aisafe.flightplan.application;

import eapli.aisafe.flightplan.domain.FlightPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlightPlanExporter {

    private static final Pattern ID_PATTERN = Pattern.compile(
            "flight\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AIRPORT_PATTERN = Pattern.compile(
            "departure\\s+(\\w{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEST_PATTERN = Pattern.compile(
            "arrival\\s+(\\w{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALTITUDE_PATTERN = Pattern.compile(
            "altitude\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AIRCRAFT_PATTERN = Pattern.compile(
            "aircraft\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEPARTURE_TIME_PATTERN = Pattern.compile(
            "departure\\s+\\w{3}\\s+(\\d{2}):(\\d{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern FUEL_PATTERN = Pattern.compile(
            "fuel\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Map<String, double[]> AIRPORT_COORDS = new java.util.HashMap<>();
    static {
        AIRPORT_COORDS.put("LIS", new double[]{38.774, -9.134});
        AIRPORT_COORDS.put("OPO", new double[]{41.248, -8.681});
        AIRPORT_COORDS.put("MAD", new double[]{40.472, -3.561});
        AIRPORT_COORDS.put("BCN", new double[]{41.297, 2.083});
        AIRPORT_COORDS.put("CDG", new double[]{49.009, 2.547});
        AIRPORT_COORDS.put("FRA", new double[]{50.033, 8.570});
        AIRPORT_COORDS.put("LHR", new double[]{51.470, -0.454});
        AIRPORT_COORDS.put("AMS", new double[]{52.308, 4.764});
        AIRPORT_COORDS.put("FNC", new double[]{32.698, -16.774});
        AIRPORT_COORDS.put("PDL", new double[]{37.741, -25.698});
        AIRPORT_COORDS.put("TER", new double[]{38.762, -27.091});
        AIRPORT_COORDS.put("FAO", new double[]{37.014, -7.965});
        AIRPORT_COORDS.put("ALC", new double[]{38.282, -0.558});
        AIRPORT_COORDS.put("AGP", new double[]{36.675, -4.499});
        AIRPORT_COORDS.put("GRO", new double[]{41.898, 2.767});
        AIRPORT_COORDS.put("IBZ", new double[]{38.873, 1.373});
        AIRPORT_COORDS.put("MAH", new double[]{39.863, 4.219});
        AIRPORT_COORDS.put("PMI", new double[]{39.553, 2.731});
        AIRPORT_COORDS.put("SCQ", new double[]{42.896, -8.415});
        AIRPORT_COORDS.put("VGO", new double[]{42.232, -8.627});
        AIRPORT_COORDS.put("BIO", new double[]{43.301, -2.911});
        AIRPORT_COORDS.put("SVQ", new double[]{37.418, -5.893});
        AIRPORT_COORDS.put("TFN", new double[]{28.483, -16.342});
        AIRPORT_COORDS.put("TFS", new double[]{28.044, -16.572});
        AIRPORT_COORDS.put("LPA", new double[]{27.932, -15.387});
        AIRPORT_COORDS.put("ACE", new double[]{28.946, -13.605});
    }

    private final FlightPlanToScenarioConverter converter;

    public FlightPlanExporter() {
        this(new FlightPlanToScenarioConverter());
    }

    FlightPlanExporter(final FlightPlanToScenarioConverter converter) {
        this.converter = converter;
    }

    public String exportForSimulator(final FlightPlan flightPlan) {
        final String dsl = flightPlan.dslContent();
        try {
            return converter.convert(dsl);
        } catch (final Exception e) {
            return exportFromDsl(dsl, flightPlan.identity().toString());
        }
    }

    private static String exportFromDsl(final String dsl, final String fallbackId) {
        final String id = extractFirst(dsl, ID_PATTERN, fallbackId);
        final String origin = extractFirst(dsl, AIRPORT_PATTERN, "LIS");
        final String dest = extractFirst(dsl, DEST_PATTERN, "OPO");
        final String departureTime = extractDepartureTime(dsl);
        final double cruiseAlt = extractAltitude(dsl);
        final int fuel = extractFuel(dsl);

        final double[] fromCoords = AIRPORT_COORDS.getOrDefault(origin, new double[]{40.0, -8.0});
        final double[] toCoords = AIRPORT_COORDS.getOrDefault(dest, new double[]{38.0, -9.0});

        final var sb = new StringBuilder();
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"ID\": \"").append(id).append("\",\n");
        sb.append("    \"Type\": \"regular\",\n");
        sb.append("    \"Route\": \"").append(origin).append("-").append(dest).append("\",\n");
        sb.append("    \"DepartureTime\": \"").append(departureTime).append("\",\n");
        sb.append("    \"DepartureTZ\": \"+00:00\",\n");
        sb.append("    \"Leg\": [\n");
        sb.append("      {\n");
        sb.append("        \"Departure\": \"").append(origin).append("\",\n");
        sb.append("        \"Arrival\": \"").append(dest).append("\",\n");
        sb.append("        \"Fuel\": { \"Quantity\": ").append(fuel).append(", \"Unit\": \"kg\" },\n");
        sb.append("        \"Flight Profile\": {\n");
        sb.append("          \"Climb\": [\n");
        sb.append("            { \"Altitude\": { \"Value\": 0, \"Unit\": \"m\" }, \"Speed\": { \"Value\": 250, \"Unit\": \"Knots\" }, \"RateClimb\": { \"Value\": 10.0, \"Unit\": \"m/s\" } },\n");
        sb.append("            { \"Altitude\": { \"Value\": ").append((int) cruiseAlt).append(", \"Unit\": \"m\" }, \"Speed\": { \"Value\": 250, \"Unit\": \"Knots\" }, \"RateClimb\": { \"Value\": 10.0, \"Unit\": \"m/s\" } }\n");
        sb.append("          ],\n");
        sb.append("          \"Descend\": [\n");
        sb.append("            { \"Altitude\": { \"Value\": ").append((int) cruiseAlt).append(", \"Unit\": \"m\" }, \"Speed\": { \"Value\": 250, \"Unit\": \"Knots\" }, \"RateDescent\": { \"Value\": -8.0, \"Unit\": \"m/s\" } },\n");
        sb.append("            { \"Altitude\": { \"Value\": 0, \"Unit\": \"m\" }, \"Speed\": { \"Value\": 250, \"Unit\": \"Knots\" }, \"RateDescent\": { \"Value\": -8.0, \"Unit\": \"m/s\" } }\n");
        sb.append("          ],\n");
        sb.append("          \"Cruise\": { \"Speed\": { \"Value\": 460, \"Unit\": \"Knots\" } }\n");
        sb.append("        },\n");
        sb.append("        \"Segments\": [\n");
        sb.append("          {\n");
        sb.append("            \"Mode\": \"climb\",\n");
        sb.append("            \"Start\": { \"Latitude\": ").append(fromCoords[0])
                .append(", \"Longitude\": ").append(fromCoords[1])
                .append(", \"Altitude\": { \"Quantity\": 0, \"Unit\": \"m\" } },\n");
        sb.append("            \"End\": { \"Latitude\": ").append(fromCoords[0] + (toCoords[0] - fromCoords[0]) * 0.3)
                .append(", \"Longitude\": ").append(fromCoords[1] + (toCoords[1] - fromCoords[1]) * 0.3)
                .append(", \"Altitude\": { \"Quantity\": ").append((int) cruiseAlt).append(", \"Unit\": \"m\" } }\n");
        sb.append("          },\n");
        sb.append("          {\n");
        sb.append("            \"Mode\": \"cruise\",\n");
        sb.append("            \"Start\": { \"Latitude\": ").append(fromCoords[0] + (toCoords[0] - fromCoords[0]) * 0.3)
                .append(", \"Longitude\": ").append(fromCoords[1] + (toCoords[1] - fromCoords[1]) * 0.3)
                .append(", \"Altitude\": { \"Quantity\": ").append((int) cruiseAlt).append(", \"Unit\": \"m\" } },\n");
        sb.append("            \"End\": { \"Latitude\": ").append(fromCoords[0] + (toCoords[0] - fromCoords[0]) * 0.7)
                .append(", \"Longitude\": ").append(fromCoords[1] + (toCoords[1] - fromCoords[1]) * 0.7)
                .append(", \"Altitude\": { \"Quantity\": ").append((int) cruiseAlt).append(", \"Unit\": \"m\" } }\n");
        sb.append("          },\n");
        sb.append("          {\n");
        sb.append("            \"Mode\": \"descend\",\n");
        sb.append("            \"Start\": { \"Latitude\": ").append(fromCoords[0] + (toCoords[0] - fromCoords[0]) * 0.7)
                .append(", \"Longitude\": ").append(fromCoords[1] + (toCoords[1] - fromCoords[1]) * 0.7)
                .append(", \"Altitude\": { \"Quantity\": ").append((int) cruiseAlt).append(", \"Unit\": \"m\" } },\n");
        sb.append("            \"End\": { \"Latitude\": ").append(toCoords[0])
                .append(", \"Longitude\": ").append(toCoords[1])
                .append(", \"Altitude\": { \"Quantity\": 0, \"Unit\": \"m\" } }\n");
        sb.append("          }\n");
        sb.append("        ]\n");
        sb.append("      }\n");
        sb.append("    ]\n");
        sb.append("  }\n");
        sb.append("]\n");
        return sb.toString();
    }

    private static String extractDepartureTime(final String dsl) {
        final Matcher m = DEPARTURE_TIME_PATTERN.matcher(dsl);
        if (m.find()) {
            return m.group(1) + ":" + m.group(2);
        }
        return "09:30";
    }

    private static double extractAltitude(final String dsl) {
        final Matcher m = ALTITUDE_PATTERN.matcher(dsl);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return 10600.0;
    }

    private static int extractFuel(final String dsl) {
        final Matcher m = FUEL_PATTERN.matcher(dsl);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 45000;
    }

    private static String extractFirst(final String dsl, final Pattern p, final String fallback) {
        final Matcher m = p.matcher(dsl);
        return m.find() ? m.group(1) : fallback;
    }
}
