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
        final List<Flight> allFlights = new ArrayList<>();
        try {
            controller.allFlights().forEach(allFlights::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
            return false;
        }

        // Show only user-imported flights (those with a flight plan matching the flight designator)
        final List<Flight> flights = allFlights.stream()
                .filter(f -> f.flightPlans().stream()
                        .anyMatch(fp -> fp.identity().toString().equals(f.identity().toString())))
                .toList();

        if (flights.isEmpty()) {
            System.out.println("  [!] No imported flights available. Import a flight plan first.");
            return false;
        }

        System.out.println("\n-- Available Flights --");
        for (int i = 0; i < flights.size(); i++) {
            final Flight f = flights.get(i);
            final String hasWeather = f.weatherDataId() != null
                    ? " [weather assigned]"
                    : "";
            System.out.printf("  %d. %s | %s | Route: %s | Pilot: %s%s%n",
                    i + 1, f.identity(), f.departureTime(),
                    f.routeName(), f.pilotLicense(), hasWeather);
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

        if (selectedFlight.weatherDataId() != null) {
            System.out.println("     (weather data #" + selectedFlight.weatherDataId()
                    + " already assigned)");
        }

        final List<WeatherData> weatherList;
        try {
            weatherList = controller.weatherDataForFlight(selectedFlight);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
            return false;
        }

        if (weatherList.isEmpty()) {
            System.out.println("  [!] No weather data available for this flight's route area.");
            System.out.println("      A Weather Person must import data for the relevant Air Control Area first.");
            return false;
        }

        System.out.println("\n-- Weather Data for this Route Area --");
        System.out.printf("  Showing %d record(s) relevant to %s route:\n\n",
                weatherList.size(), selectedFlight.routeName());
        for (int i = 0; i < weatherList.size(); i++) {
            final WeatherData w = weatherList.get(i);
            System.out.printf("  %d. [%d] %s | T=%.1f°C | Wind: %.0f kt @ %d°%n",
                    i + 1, w.identity(), w.areaCode(),
                    w.temperatureCelsius(),
                    w.windCondition().speedKnots(),
                    w.windCondition().directionDegrees());
            System.out.printf("        Provider: %s | Date: %s | Alt: %dm%n",
                    w.sourceProvider(), w.recordedDateTime(),
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
