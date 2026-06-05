package eapli.aisafe.ui.flight;

import eapli.aisafe.flight.application.AddWeatherToFlightController;
import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("squid:S106")
public class AddWeatherToFlightUI extends AbstractUI {

    private final AddWeatherToFlightController controller =
            new AddWeatherToFlightController();

    @Override
    protected boolean doShow() {
        final List<Flight> flights = new ArrayList<>();
        try {
            controller.allFlights().forEach(flights::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
            return false;
        }

        if (flights.isEmpty()) {
            System.out.println("  [!] No flights registered.");
            return false;
        }

        System.out.println("\n-- Available Flights --");
        for (int i = 0; i < flights.size(); i++) {
            final Flight f = flights.get(i);
            System.out.printf("  %d. %s | %s | Route: %s | Pilot: %s%n",
                    i + 1, f.identity(), f.departureTime(),
                    f.routeName(), f.pilotLicense());
            if (f.weatherDataId() != null) {
                System.out.printf("        (weather data #%d already assigned)%n", f.weatherDataId());
            }
        }

        Flight selectedFlight = null;
        while (selectedFlight == null) {
            final int opt = Console.readInteger("Select flight (1-" + flights.size() + ")");
            if (opt < 1 || opt > flights.size()) {
                System.out.println("  [!] Invalid option.");
                continue;
            }
            selectedFlight = flights.get(opt - 1);
        }
        System.out.println("  >> Flight: " + selectedFlight.identity());

        final List<WeatherData> weatherList = new ArrayList<>();
        try {
            controller.allWeatherData().forEach(weatherList::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
            return false;
        }

        if (weatherList.isEmpty()) {
            System.out.println("  [!] No weather data available. A Weather Person must import data first.");
            return false;
        }

        System.out.println("\n-- Available Weather Data --");
        System.out.println("  Select the weather forecast to associate with this flight:\n");
        for (int i = 0; i < weatherList.size(); i++) {
            final WeatherData w = weatherList.get(i);
            System.out.printf("  %d. [%d] %s | T=%.1f°C | Wind: %.0f kt @ %d°%n",
                    i + 1, w.identity(), w.areaCode(),
                    w.temperatureCelsius(),
                    w.windCondition().speedKnots(),
                    w.windCondition().directionDegrees());
            System.out.printf("        Provider: %s | Date: %s | Lat/Lon/Alt: %.4f/%.4f/%dm%n",
                    w.sourceProvider(), w.recordedDateTime(),
                    w.windCondition().latitude(),
                    w.windCondition().longitude(),
                    w.windCondition().altitudeMetres());
        }

        WeatherData selectedWeather = null;
        while (selectedWeather == null) {
            final int opt = Console.readInteger("Select weather data (1-" + weatherList.size() + ")");
            if (opt < 1 || opt > weatherList.size()) {
                System.out.println("  [!] Invalid option.");
                continue;
            }
            selectedWeather = weatherList.get(opt - 1);
        }
        System.out.println("  >> Weather data #" + selectedWeather.identity()
                + " (" + selectedWeather.areaCode() + ")");

        try {
            final Flight updated = controller.assignWeather(
                    selectedFlight.identity().toString(),
                    selectedWeather.identity());
            System.out.println("\n  >> Weather data assigned to flight " + updated.identity());
            if (updated.flightPlans().stream()
                    .anyMatch(fp -> fp.status().name().equals("DRAFT")
                            && fp.createdAt() != fp.lastTestedAt())) {
                System.out.println("     Flight plans in TEST_PASSED/TEST_FAILED status were reset to DRAFT.");
            }
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Add Weather Data to Flight (US082)";
    }
}
