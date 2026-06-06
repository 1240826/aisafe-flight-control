package eapli.aisafe.ui.flight;

import eapli.aisafe.flight.application.FetchWeatherForFlightController;
import eapli.aisafe.flight.domain.Flight;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("squid:S106")
public class FetchWeatherForFlightUI extends AbstractUI {

    private final FetchWeatherForFlightController controller =
            new FetchWeatherForFlightController();

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

        System.out.println("\n-- Select a Flight to Fetch Weather For --");
        for (int i = 0; i < flights.size(); i++) {
            final Flight f = flights.get(i);
            final String hasWeather = f.weatherDataId() != null
                    ? " [weather #" + f.weatherDataId() + "]"
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

        final var midpoint = controller.computeMidpoint(selectedFlight);
        System.out.printf("  >> Route: %s-%s | Midpoint: %.3f, %.3f%n",
                midpoint.originAirport(), midpoint.destinationAirport(),
                midpoint.latitude(), midpoint.longitude());

        System.out.print("  >> Detecting Air Control Area... ");
        final var aca = controller.findAcaForMidpoint(midpoint.latitude(), midpoint.longitude());
        System.out.println(aca.code() + " (auto-detected)");

        System.out.println("  >> Fetching weather from Open-Meteo API...");
        try {
            final var result = controller.fetchAndAssignWeather(
                    selectedFlight.identity().toString(),
                    aca.code().toString(), midpoint.latitude(), midpoint.longitude());

            System.out.println("  >> Weather fetched successfully:");
            System.out.println("     Weather data ID: " + result.weatherDataId());
            System.out.println("     Altitude zones:  " + result.zoneCount());
            System.out.println("     ACA: " + aca.code());
            System.out.println("     Assigned to flight: " + selectedFlight.identity());

            final String filePath = controller.writeWeatherFile(result.weatherJson());
            System.out.println("  >> Weather data ready for simulator.");
        } catch (final Exception e) {
            System.out.println("  [!] Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Fetch Weather for Flight (Weather API Integration)";
    }
}
