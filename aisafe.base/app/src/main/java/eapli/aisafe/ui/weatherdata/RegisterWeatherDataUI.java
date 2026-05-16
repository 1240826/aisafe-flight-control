package eapli.aisafe.ui.weatherdata;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.weatherdata.application.RegisterWeatherDataController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * UI for US041 — Register Weather Data.
 * US042: the user identifies the data source (sourceProvider).
 * Each observation is recorded at a specific geographic coordinate, altitude,
 * and instant in time (recordedDateTime) within an ACA.
 */
@SuppressWarnings("squid:S106")
public class RegisterWeatherDataUI extends AbstractUI {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RegisterWeatherDataController controller =
            new RegisterWeatherDataController();

    @Override
    protected boolean doShow() {
        // --- 1. Pick ACA from the list of registered areas ---
        final List<AirControlArea> areas = new ArrayList<>();
        controller.allAirControlAreas().forEach(areas::add);

        if (areas.isEmpty()) {
            System.out.println("  [!] No Air Control Areas are registered yet. Please register one first.");
            return false;
        }

        System.out.println("\nAvailable Air Control Areas:");
        for (int i = 0; i < areas.size(); i++) {
            final AirControlArea aca = areas.get(i);
            System.out.printf("  %d. %-10s %s%n", i + 1, aca.code(), aca.name());
        }

        int acaIdx;
        do {
            acaIdx = Console.readInteger("Select Air Control Area (1-" + areas.size() + ")");
            if (acaIdx < 1 || acaIdx > areas.size()) {
                System.out.println("  [!] Please enter a number between 1 and " + areas.size() + ".");
            }
        } while (acaIdx < 1 || acaIdx > areas.size());

        final AirControlArea selectedAca = areas.get(acaIdx - 1);
        final String areaCode = selectedAca.code().toString();

        // Show ACA boundary as reference for the coordinate
        System.out.printf("%n  [i] ACA '%s' covers:%n", areaCode);
        System.out.printf("      Lat: %.4f to %.4f | Lon: %.4f to %.4f%n",
                selectedAca.minLat(), selectedAca.maxLat(),
                selectedAca.minLon(), selectedAca.maxLon());
        System.out.println("      Observation coordinates should be within these bounds.\n");

        // --- 2. Observation coordinate (single point, not a rectangle) ---
        System.out.println("-- Observation location --");

        double lat;
        do {
            lat = Console.readDouble("  Latitude (-90 to 90)");
            if (lat < -90 || lat > 90) {
                System.out.println("  [!] Latitude must be between -90 and 90.");
            }
        } while (lat < -90 || lat > 90);

        double lon;
        do {
            lon = Console.readDouble("  Longitude (-180 to 180)");
            if (lon < -180 || lon > 180) {
                System.out.println("  [!] Longitude must be between -180 and 180.");
            }
        } while (lon < -180 || lon > 180);

        int alt;
        do {
            alt = Console.readInteger("  Altitude (m, must be >= 0)");
            if (alt < 0) {
                System.out.println("  [!] Altitude must be non-negative.");
            }
        } while (alt < 0);

        // --- 3. Wind ---
        System.out.println("-- Wind --");
        double windSpeed;
        do {
            windSpeed = Console.readDouble("  Speed (kt, must be > 0)");
            if (windSpeed <= 0) {
                System.out.println("  [!] Wind speed must be strictly positive (> 0).");
            }
        } while (windSpeed <= 0);

        double windDir;
        do {
            windDir = Console.readDouble("  Direction (deg, 0 to <360)");
            if (windDir < 0 || windDir >= 360) {
                System.out.println("  [!] Wind direction must be between 0 (inclusive) and 360 (exclusive).");
            }
        } while (windDir < 0 || windDir >= 360);

        // --- 4. Temperature (any value — can be negative) ---
        final double temp = Console.readDouble("Temperature (°C)");

        // --- 5. Source provider (US042) ---
        String sourceProvider = null;
        while (sourceProvider == null) {
            final String input = Console.readLine("Source Provider (e.g. 'IPMA', 'METAR LPPC', 'EUROCONTROL')").trim();
            if (input.isBlank()) {
                System.out.println("  [!] Source provider cannot be blank.");
            } else {
                sourceProvider = input;
            }
        }

        // --- 6. Observation date/time (single instant) ---
        LocalDateTime recordedDateTime = null;
        while (recordedDateTime == null) {
            try {
                recordedDateTime = LocalDateTime.parse(
                        Console.readLine("Recorded Date/Time (yyyy-MM-dd HH:mm)").trim(), DT_FMT);
            } catch (final DateTimeParseException e) {
                System.out.println("  [!] Invalid format. Use yyyy-MM-dd HH:mm (e.g. 2027-06-01 14:30).");
            }
        }

        try {
            controller.registerWeatherData(areaCode,
                    lat, lon, alt,
                    windSpeed, windDir,
                    temp,
                    sourceProvider,
                    recordedDateTime);
            System.out.println("  >> Weather data registered successfully.");
        } catch (final Exception e) {
            System.out.println("  [!] Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Register Weather Data (US041)";
    }
}
