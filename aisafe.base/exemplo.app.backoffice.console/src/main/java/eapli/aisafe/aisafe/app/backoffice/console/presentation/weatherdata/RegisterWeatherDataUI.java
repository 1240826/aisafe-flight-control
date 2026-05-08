package eapli.aisafe.app.backoffice.console.presentation.weatherdata;

import eapli.aisafe.weatherdata.application.RegisterWeatherDataController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * UI for US041 — Register Weather Data.
 */
@SuppressWarnings("squid:S106")
public class RegisterWeatherDataUI extends AbstractUI {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RegisterWeatherDataController controller =
            new RegisterWeatherDataController();

    @Override
    protected boolean doShow() {
        final String areaCode = Console.readLine("Air Control Area Code");
        System.out.println("-- Sub-area boundaries --");
        final double minLat  = Console.readDouble("  Min Latitude");
        final double maxLat  = Console.readDouble("  Max Latitude");
        final double minLon  = Console.readDouble("  Min Longitude");
        final double maxLon  = Console.readDouble("  Max Longitude");
        final double minAlt  = Console.readDouble("  Min Altitude (m)");
        final double maxAlt  = Console.readDouble("  Max Altitude (m)");
        System.out.println("-- Wind --");
        final double windSpeed = Console.readDouble("  Speed (kt)");
        final double windDir   = Console.readDouble("  Direction (deg, 0-360)");
        final double temp      = Console.readDouble("Temperature (°C)");
        final String fromStr   = Console.readLine("Valid From (yyyy-MM-dd HH:mm)");
        final String toStr     = Console.readLine("Valid To   (yyyy-MM-dd HH:mm)");

        try {
            final LocalDateTime validFrom = LocalDateTime.parse(fromStr, DT_FMT);
            final LocalDateTime validTo   = LocalDateTime.parse(toStr, DT_FMT);
            controller.registerWeatherData(areaCode,
                    minLat, maxLat, minLon, maxLon, minAlt, maxAlt,
                    windSpeed, windDir, temp, validFrom, validTo);
            System.out.println("Weather data registered successfully.");
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Register Weather Data (US041)";
    }
}
