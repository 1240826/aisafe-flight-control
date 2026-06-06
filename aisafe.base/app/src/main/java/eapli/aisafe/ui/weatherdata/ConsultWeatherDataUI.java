package eapli.aisafe.ui.weatherdata;

import eapli.aisafe.weatherdata.application.ConsultWeatherDataController;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * UI for US043 - Consult Weather Data
 * Actor: Weather Person, Pilot, Flight Control Operator
 */
@SuppressWarnings("squid:S106")
public class ConsultWeatherDataUI extends AbstractUI {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ConsultWeatherDataController controller = new ConsultWeatherDataController();

    @Override
    protected boolean doShow() {
        final String areaCode = Console.readLine("Area Code (e.g. LPPC))").trim().toUpperCase();
        if (areaCode.isBlank()) {
            System.out.println("Area code cannot be blank.");
            return false;
        }

        LocalDate date = null;
        while (date == null) {
            try {
                date = LocalDate.parse(
                        Console.readLine("Date (yyyy-MM-dd)").trim(), DATE_FMT);
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid date format. Use dd-mm-yyyy.");
            }
        }

        try {
            final Iterable<WeatherData> results = controller.consultWeatherData(areaCode, date);
            boolean found = false;
            for (final WeatherData wd : results) {
                System.out.printf(" [%s] %s | T=%.1fºC | Wind %s | Source: %s%n",
                        wd.recordedDateTime(), wd.areaCode(),
                        wd.temperatureCelsius(), wd.windCondition(), wd.sourceProvider());
                found = true;
            }

            if (!found) {
                System.out.println(" >> No weather data found for area " + areaCode + " on " + date + ".");
            }
        } catch (final Exception e) {
            System.out.println(" Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline(){
        return "Consult Weather Data";
    }
}
