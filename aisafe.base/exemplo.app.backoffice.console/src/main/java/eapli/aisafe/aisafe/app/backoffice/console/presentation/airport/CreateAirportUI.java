package eapli.aisafe.app.backoffice.console.presentation.airport;

import eapli.aisafe.airport.application.CreateAirportController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI for US052 — Create Airport.
 */
@SuppressWarnings("squid:S106")
public class CreateAirportUI extends AbstractUI {

    private final CreateAirportController controller = new CreateAirportController();

    @Override
    protected boolean doShow() {
        System.out.println("\nAvailable Air Control Areas:");
        controller.allAirControlAreas().forEach(a -> System.out.println("  " + a));

        final String iata    = Console.readLine("IATA Code (3 letters)");
        final String icao    = Console.readLine("ICAO Code (4 letters)");
        final String name    = Console.readLine("Airport Name");
        final String city    = Console.readLine("City");
        final String country = Console.readLine("Country");
        final double lat     = Console.readDouble("Latitude");
        final double lon     = Console.readDouble("Longitude");
        final String aca     = Console.readLine("Air Control Area Code");

        try {
            controller.createAirport(iata, icao, name, city, country, lat, lon, aca);
            System.out.println("Airport created successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Error: duplicate IATA/ICAO code.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Create Airport (US052)";
    }
}
