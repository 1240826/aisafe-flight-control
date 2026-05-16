package eapli.aisafe.ui.airport;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.airport.application.CreateAirportController;
import eapli.aisafe.airport.domain.CountryList;
import eapli.aisafe.airport.domain.Elevation;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * UI for US052 - Create Airport.
 */
@SuppressWarnings("squid:S106")
public class CreateAirportUI extends AbstractUI {

    private final CreateAirportController controller = new CreateAirportController();

    @Override
    protected boolean doShow() {
        // --- ACA selection ---
        final List<AirControlArea> acas = new ArrayList<>();
        try {
            controller.allAirControlAreas().forEach(acas::add);
        } catch (final IllegalStateException | IllegalArgumentException e) {
            System.out.println("  [!] Could not load Air Control Areas: " + e.getMessage());
            return false;
        }
        if (acas.isEmpty()) {
            System.out.println("  [!] No Air Control Areas registered. Please register one first.");
            return false;
        }
        System.out.println("\nAir Control Areas:");
        for (int i = 0; i < acas.size(); i++) {
            System.out.printf("  %d. %s - %s%n", i + 1,
                    acas.get(i).identity(), acas.get(i).name());
        }
        int acaIdx;
        do {
            acaIdx = Console.readInteger("Select ACA (1-" + acas.size() + ")");
            if (acaIdx < 1 || acaIdx > acas.size()) {
                System.out.println("  [!] Please enter a number between 1 and " + acas.size() + ".");
            }
        } while (acaIdx < 1 || acaIdx > acas.size());
        final AirControlArea selectedAca = acas.get(acaIdx - 1);
        final String acaCode = selectedAca.identity().toString();
        System.out.printf("%n  [i] ACA '%s' boundary:%n", selectedAca.identity());
        System.out.printf("      Latitude : %.4f to %.4f%n", selectedAca.minLat(), selectedAca.maxLat());
        System.out.printf("      Longitude: %.4f to %.4f%n", selectedAca.minLon(), selectedAca.maxLon());
        System.out.println("      [!] Airport coordinates MUST fall within these bounds.");

        // IATA Code - exactly 3 uppercase letters
        String iata;
        do {
            iata = Console.readLine("IATA Code (3 uppercase letters, e.g. LIS)").trim().toUpperCase();
            if (!iata.matches("[A-Z]{3}")) {
                System.out.println("  [!] IATA Code must be exactly 3 uppercase letters (e.g. LIS).");
            }
        } while (!iata.matches("[A-Z]{3}"));

        // ICAO Code — exactly 4 uppercase letters
        String icao;
        do {
            icao = Console.readLine("ICAO Code (4 uppercase letters, e.g. LPPT)").trim().toUpperCase();
            if (!icao.matches("[A-Z]{4}")) {
                System.out.println("  [!] ICAO Code must be exactly 4 uppercase letters (e.g. LPPT).");
            }
        } while (!icao.matches("[A-Z]{4}"));

        // Airport Name — must contain at least one letter
        String name = null;
        while (name == null) {
            final String nameInput = Console.readLine("Airport Name (e.g. 'Humberto Delgado Airport')").trim();
            if (nameInput.isBlank()) {
                System.out.println("  [!] Airport Name cannot be blank.");
            } else if (!nameInput.matches(".*\\p{L}.*")) {
                System.out.println("  [!] Airport Name must contain at least one letter.");
            } else {
                name = nameInput;
            }
        }

        // City — must contain at least one letter
        String city = null;
        while (city == null) {
            final String cityInput = Console.readLine("City (e.g. 'Lisbon')").trim();
            if (cityInput.isBlank()) {
                System.out.println("  [!] City cannot be blank.");
            } else if (!cityInput.matches(".*\\p{L}.*")) {
                System.out.println("  [!] City must contain at least one letter.");
            } else {
                city = cityInput;
            }
        }

        // Country — selected from bootstrapped list (clarification §17)
        System.out.println("Countries:");
        for (int i = 0; i < CountryList.ALL.length; i++) {
            System.out.printf("  %2d. %s%n", i + 1, CountryList.ALL[i]);
        }
        int countryIdx;
        do {
            countryIdx = Console.readInteger("Select country (1-" + CountryList.ALL.length + ")");
            if (countryIdx < 1 || countryIdx > CountryList.ALL.length) {
                System.out.println("  [!] Please enter a number between 1 and " + CountryList.ALL.length + ".");
            }
        } while (countryIdx < 1 || countryIdx > CountryList.ALL.length);
        final String country = CountryList.ALL[countryIdx - 1];

        // Latitude — -90 to 90
        double lat;
        do {
            lat = Console.readDouble("Latitude (-90 to 90)");
            if (lat < -90 || lat > 90) {
                System.out.println("  [!] Latitude must be between -90 and 90.");
            }
        } while (lat < -90 || lat > 90);

        // Longitude — -180 to 180
        double lon;
        do {
            lon = Console.readDouble("Longitude (-180 to 180)");
            if (lon < -180 || lon > 180) {
                System.out.println("  [!] Longitude must be between -180 and 180.");
            }
        } while (lon < -180 || lon > 180);

        // Elevation — ask value first, then unit
        final double elev = Console.readDouble("Elevation value (airports can be below sea level, e.g. 113)");
        System.out.println("  Elevation unit:");
        for (int i = 0; i < Elevation.VALID_UNITS.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, Elevation.VALID_UNITS[i]);
        }
        int elevUnitIdx;
        do {
            elevUnitIdx = Console.readInteger("  Select unit (1-" + Elevation.VALID_UNITS.length + ")");
            if (elevUnitIdx < 1 || elevUnitIdx > Elevation.VALID_UNITS.length) {
                System.out.println("  [!] Please enter a number between 1 and " + Elevation.VALID_UNITS.length + ".");
            }
        } while (elevUnitIdx < 1 || elevUnitIdx > Elevation.VALID_UNITS.length);
        final String elevUnit = Elevation.VALID_UNITS[elevUnitIdx - 1];

        try {
            controller.createAirport(iata, icao, name, city, country, lat, lon, elev, elevUnit, acaCode);
            System.out.println("  >> Airport created successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("  [!] Duplicate IATA/ICAO code.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Create Airport (US052)";
    }
}
