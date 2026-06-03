package eapli.aisafe.flightplan.application;

import eapli.aisafe.flightplan.domain.ValidationResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class DslValidator {

    private static final Pattern AIRPORT_CODE = Pattern.compile("[A-Za-z]{3}");
    private static final Pattern AIRCRAFT_MODEL = Pattern.compile("[A-Za-z0-9]+");
    private static final Pattern FLIGHT_NUMBER = Pattern.compile("[A-Za-z0-9]+");
    private static final Pattern TIME = Pattern.compile("\\d{2}:\\d{2}");
    private static final Pattern ASCII_ONLY = Pattern.compile("\\A\\p{ASCII}*\\z");
    private static final Pattern UPPERCASE_COORD = Pattern.compile("[A-Z]{4}");

    private static final Set<String> VALID_TYPES = Set.of("regular", "charter");
    private static final Set<String> VALID_ENGINE_TYPES = Set.of("jet", "propeller", "turboprop");
    private static final Set<String> VALID_WAKE_CATEGORIES = Set.of("l", "m", "h");

    private static final Set<String> VALID_KEYWORDS = Set.of(
            "departure", "arrival", "type", "aircraft", "fuel", "flight", "altitude",
            "speed", "remarks", "segment", "waypoint", "route", "engine", "passengers",
            "wake");

    private final FlightPlanToScenarioConverter converter;

    public DslValidator() {
        this(new FlightPlanToScenarioConverter());
    }

    DslValidator(final FlightPlanToScenarioConverter converter) {
        this.converter = converter;
    }

    public ValidationResult validate(final String dslContent) {
        if (dslContent == null || dslContent.isBlank()) {
            return ValidationResult.failed("DSL content is empty");
        }

        if (!ASCII_ONLY.matcher(dslContent).matches()) {
            return ValidationResult.failed("DSL contains non-ASCII characters");
        }

        // Try ANTLR-based format first
        if (converter.canConvert(dslContent)) {
            return ValidationResult.passed();
        }

        final var trimmed = dslContent.trim();
        final var segments = trimmed.split(";");
        if (segments.length < 2) {
            return ValidationResult.failed(
                    "DSL must contain at least two segments separated by ';'");
        }

        final var reasons = new ArrayList<String>();
        final var seenKeywords = new HashSet<String>();

        for (int i = 0; i < segments.length; i++) {
            final var segment = segments[i].trim();
            if (segment.isEmpty()) continue;

            final var kv = segment.split("\\s+", 2);
            final var keyword = kv[0].toLowerCase();

            if (!VALID_KEYWORDS.contains(keyword)) {
                reasons.add("Unknown keyword: '" + kv[0] + "'");
                continue;
            }

            if (seenKeywords.contains(keyword)) {
                reasons.add("Duplicate keyword: '" + kv[0] + "'");
            }
            seenKeywords.add(keyword);

            final var value = kv.length > 1 ? kv[1].trim() : "";

            switch (keyword) {
                case "departure":
                case "arrival":
                    if (value.isEmpty()) {
                        reasons.add("'" + kv[0] + "' requires an airport code and time");
                    } else {
                        final var vParts = value.split("\\s+", 2);
                        if (vParts.length < 2 || vParts[1].isEmpty()) {
                            reasons.add("'" + kv[0] + "' requires an airport code and time (HH:MM)");
                        } else {
                            if (!AIRPORT_CODE.matcher(vParts[0]).matches()) {
                                reasons.add("Invalid airport code in '" + kv[0] + "': " + vParts[0]);
                            }
                            if (!TIME.matcher(vParts[1]).matches()) {
                                reasons.add("Invalid time in '" + kv[0] + "': " + vParts[1]);
                            }
                        }
                    }
                    break;
                case "type":
                    if (!VALID_TYPES.contains(value.toLowerCase())) {
                        reasons.add("Invalid flight type: '" + value
                                + "' (expected 'regular' or 'charter')");
                    }
                    break;
                case "aircraft":
                    if (value.isEmpty() || !AIRCRAFT_MODEL.matcher(value).matches()) {
                        reasons.add("Invalid aircraft model code: '" + value + "'");
                    }
                    break;
                case "fuel":
                    if (value.isEmpty()) {
                        reasons.add("Fuel value is empty");
                    } else {
                        try {
                            final var fuelVal = Integer.parseInt(value);
                            if (fuelVal < 0) {
                                reasons.add("Fuel value cannot be negative: " + fuelVal);
                            }
                        } catch (final NumberFormatException e) {
                            reasons.add("Invalid fuel value: '" + value + "' (expected integer)");
                        }
                    }
                    break;
                case "flight":
                    if (value.isEmpty() || !FLIGHT_NUMBER.matcher(value).matches()) {
                        reasons.add("Invalid flight number: '" + value + "'");
                    }
                    break;
                case "altitude":
                    if (value.isEmpty()) {
                        reasons.add("Altitude value is empty");
                    } else {
                        try {
                            final var altVal = Integer.parseInt(value);
                            if (altVal < 0) {
                                reasons.add("Altitude cannot be negative: " + altVal);
                            } else if (altVal > 60000) {
                                reasons.add("Altitude exceeds maximum (60000 ft): " + altVal);
                            }
                        } catch (final NumberFormatException e) {
                            reasons.add("Invalid altitude value: '" + value + "' (expected integer)");
                        }
                    }
                    break;
                case "speed":
                    if (value.isEmpty()) {
                        reasons.add("Speed value is empty");
                    } else {
                        try {
                            final var speedVal = Integer.parseInt(value);
                            if (speedVal < 0) {
                                reasons.add("Speed cannot be negative: " + speedVal);
                            } else if (speedVal > 700) {
                                reasons.add("Speed exceeds maximum (700 knots): " + speedVal);
                            }
                        } catch (final NumberFormatException e) {
                            reasons.add("Invalid speed value: '" + value + "' (expected integer)");
                        }
                    }
                    break;
                case "engine":
                    if (!VALID_ENGINE_TYPES.contains(value.toLowerCase())) {
                        reasons.add("Invalid engine type: '" + value
                                + "' (expected 'jet', 'propeller', or 'turboprop')");
                    }
                    break;
                case "wake":
                    if (!VALID_WAKE_CATEGORIES.contains(value.toLowerCase())) {
                        reasons.add("Invalid wake turbulence category: '" + value
                                + "' (expected 'L', 'M', or 'H')");
                    }
                    break;
                case "passengers":
                    if (value.isEmpty()) {
                        reasons.add("Passengers value is empty");
                    } else {
                        try {
                            final var paxVal = Integer.parseInt(value);
                            if (paxVal <= 0) {
                                reasons.add("Passengers must be positive: " + paxVal);
                            }
                        } catch (final NumberFormatException e) {
                            reasons.add("Invalid passengers value: '" + value + "' (expected integer)");
                        }
                    }
                    break;
                case "waypoint":
                    if (value.isEmpty()) {
                        reasons.add("Waypoint value is empty");
                    } else {
                        if (!UPPERCASE_COORD.matcher(value).matches()) {
                            reasons.add("Invalid waypoint format: '" + value
                                    + "' (expected 4-letter uppercase code)");
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        if (!reasons.isEmpty()) {
            return ValidationResult.failed(reasons);
        }

        return ValidationResult.passed();
    }
}