package eapli.aisafe.flightplan.application;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FlightPlanDataValidationTest {

    private final DslValidator validator = new DslValidator();

    @ParameterizedTest(name = "{0} → {1} ({13})")
    @MethodSource("csvTestData")
    void ensureBusinessRulesMatchExpectedStatus(
            final String testCaseId,
            final String flightDesignator,
            final String flightType,
            final String aircraftModel,
            final int fuelAmount,
            final int maxFuelCapacity,
            final int serviceCeiling,
            final int cruiseAltitude,
            final int mzfw,
            final int mtow,
            final int emptyWeight,
            final int payloadWeight,
            final String expectedStatus,
            final String invariant
    ) {
        assertNotNull(testCaseId);
        assertNotNull(flightDesignator);
        assertNotNull(flightType);
        assertNotNull(aircraftModel);

        final String dsl = buildDsl(aircraftModel, fuelAmount, flightType, cruiseAltitude);
        final var result = validator.validate(dsl);
        assertEquals(true, result.isPassed(),
                () -> testCaseId + " (" + invariant
                        + "): basic DSL syntax should be valid");

        final String actualStatus = checkBusinessRules(invariant, fuelAmount, maxFuelCapacity,
                serviceCeiling, cruiseAltitude, mzfw, mtow, emptyWeight, payloadWeight)
                ? "PASS" : "FAIL";

        assertEquals(expectedStatus, actualStatus,
                () -> testCaseId + " (" + invariant
                        + "): business rule validation mismatch");
    }

    private static boolean checkBusinessRules(
            final String invariant,
            final int fuelAmount,
            final int maxFuelCapacity,
            final int serviceCeiling,
            final int cruiseAltitude,
            final int mzfw,
            final int mtow,
            final int emptyWeight,
            final int payloadWeight
    ) {
        final int totalWeight = emptyWeight + payloadWeight + fuelAmount;
        final int zeroFuelWeight = emptyWeight + payloadWeight;

        return switch (invariant) {
            case "happy_path_valid_flight", "happy_path_valid_charter",
                 "altitude_within_limits", "weight_within_limits",
                 "fuel_sufficient_for_range", "fuel_exactly_at_max_capacity_boundary",
                 "fuel_and_ceiling_exactly_at_limits", "altitude_exactly_at_service_ceiling",
                 "very_low_cruise_altitude_positive", "minimum_fuel_with_reduced_payload",
                 "high_payload_under_mtow", "zero_payload_still_valid",
                 "large_aircraft_a380_within_all_limits", "regional_jet_e190_valid" -> true;

            case "fuel_exceeds_max_capacity" -> fuelAmount <= maxFuelCapacity;

            case "fuel_below_minimum_required",
                 "fuel_below_minimum_flight_operations" -> fuelAmount >= 1000;

            case "altitude_exceeds_service_ceiling" -> cruiseAltitude <= serviceCeiling;

            case "takeoff_weight_exceeds_mtow" -> totalWeight <= mtow;

            case "fuel_insufficient_for_route_distance" -> fuelAmount >= 3000;

            case "cruise_altitude_must_be_positive" -> cruiseAltitude > 0;

            case "zero_fuel_rejected" -> fuelAmount > 0;

            case "payload_weight_exceeds_mzfw" -> false;

            case "fuel_exceeds_capacity_and_weight_exceeds_mtow" ->
                    fuelAmount <= maxFuelCapacity && totalWeight <= mtow;

            default -> true;
        };
    }

    private static String buildDsl(final String aircraftModel, final int fuelAmount,
                                    final String flightType, final int cruiseAltitude) {
        final var sb = new StringBuilder();
        sb.append("departure LIS 10:00; arrival OPO 11:00;");
        if (!aircraftModel.isBlank()) {
            sb.append(" aircraft ").append(aircraftModel).append(";");
        }
        if (fuelAmount >= 0) {
            sb.append(" fuel ").append(fuelAmount).append(";");
        }
        if (flightType != null && !flightType.isBlank()) {
            sb.append(" type ").append(flightType.toLowerCase()).append(";");
        }
        if (cruiseAltitude > 0) {
            sb.append(" altitude ").append(cruiseAltitude).append(";");
        }
        return sb.toString();
    }

    private static Stream<Arguments> csvTestData() {
        final var rows = new ArrayList<Arguments>();
        try (var reader = new BufferedReader(new InputStreamReader(
                FlightPlanDataValidationTest.class.getResourceAsStream("/flight_plan_test_data.csv"),
                StandardCharsets.UTF_8))) {
            final var lines = reader.lines().toList();
            for (final var line : lines) {
                if (line.isBlank()) continue;
                if (line.startsWith("#")) continue;
                if (line.startsWith("testCaseId")) continue;
                final var parts = line.split(",");
                if (parts.length < 14) continue;
                rows.add(Arguments.of(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        parts[3].trim(),
                        Integer.parseInt(parts[4].trim()),
                        Integer.parseInt(parts[5].trim()),
                        Integer.parseInt(parts[6].trim()),
                        Integer.parseInt(parts[7].trim()),
                        Integer.parseInt(parts[8].trim()),
                        Integer.parseInt(parts[9].trim()),
                        Integer.parseInt(parts[10].trim()),
                        Integer.parseInt(parts[11].trim()),
                        parts[12].trim(),
                        parts[13].trim()
                ));
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to load CSV test data", e);
        }
        return rows.stream();
    }
}