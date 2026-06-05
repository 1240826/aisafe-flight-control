package eapli.aisafe.ui.flightroute;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.flightroute.application.CreateFlightRouteController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("squid:S106")
public class CreateFlightRouteUI extends AbstractUI {

    private final CreateFlightRouteController controller = new CreateFlightRouteController();

    @Override
    protected boolean doShow() {
        System.out.println();

        // ── List Companies ───────────────────────────────────────────────
        final List<AirTransportCompany> companies = new ArrayList<>();
        try {
            controller.allCompanies().forEach(companies::add);
        } catch (final Exception e) {
            System.out.println("  [!] Could not load companies: " + e.getMessage());
            return false;
        }
        if (companies.isEmpty()) {
            System.out.println("  [!] No companies registered. Please register one first.");
            return false;
        }
        System.out.println("Available Companies:");
        for (int i = 0; i < companies.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, companies.get(i));
        }

        int companyIdx;
        do {
            companyIdx = Console.readInteger("Select company (1-" + companies.size() + ")");
            if (companyIdx < 1 || companyIdx > companies.size()) {
                System.out.println("  [!] Invalid option.");
            }
        } while (companyIdx < 1 || companyIdx > companies.size());
        final String companyIata = companies.get(companyIdx - 1).iata().toString();

        // ── List Airports ────────────────────────────────────────────────
        final List<Airport> airports = new ArrayList<>();
        try {
            controller.allAirports().forEach(airports::add);
        } catch (final Exception e) {
            System.out.println("  [!] Could not load airports: " + e.getMessage());
            return false;
        }
        if (airports.size() < 2) {
            System.out.println("  [!] At least 2 airports are required. Please register more first.");
            return false;
        }

        System.out.println("\nAvailable Airports:");
        for (int i = 0; i < airports.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, airports.get(i));
        }

        // ── Origin ───────────────────────────────────────────────────────
        int originIdx;
        do {
            originIdx = Console.readInteger("Select origin airport (1-" + airports.size() + ")");
            if (originIdx < 1 || originIdx > airports.size()) {
                System.out.println("  [!] Invalid option.");
            }
        } while (originIdx < 1 || originIdx > airports.size());
        final String originCode = airports.get(originIdx - 1).iata().toString();

        // ── Destination (exclude origin) ─────────────────────────────────
        final List<Airport> destAirports = new ArrayList<>(airports);
        destAirports.remove(originIdx - 1);

        int destIdx;
        do {
            System.out.println("\nAvailable destinations (origin excluded):");
            for (int i = 0; i < destAirports.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, destAirports.get(i));
            }
            destIdx = Console.readInteger("Select destination airport (1-" + destAirports.size() + ")");
            if (destIdx < 1 || destIdx > destAirports.size()) {
                System.out.println("  [!] Invalid option.");
            }
        } while (destIdx < 1 || destIdx > destAirports.size());
        final String destCode = destAirports.get(destIdx - 1).iata().toString();

        // ── Route Name (loop until valid + unique) ───────────────────────
        while (true) {
            final String raw = Console.readLine("Route Name (e.g. TP123)").trim().toUpperCase();
            if (raw.isBlank()) {
                System.out.println("  [!] Route name cannot be blank.");
                continue;
            }
            try {
                if (controller.routeExists(raw)) {
                    System.out.println("  [!] Route '" + raw + "' already exists.");
                    if (!askYesNo("Try another name?")) return false;
                    continue;
                }
            } catch (final IllegalArgumentException | IllegalStateException e) {
                System.out.println("  [!] " + e.getMessage());
                continue;
            }

            // ── Save ─────────────────────────────────────────────────────
            try {
                controller.createFlightRoute(raw, companyIata, originCode, destCode);
                System.out.println("  >> Flight route " + raw + " created successfully.");
                return false;
            } catch (final IntegrityViolationException | ConcurrencyException e) {
                System.out.println("  [!] Route name already exists (concurrent creation).");
                if (!askYesNo("Try another name?")) return false;
            } catch (final IllegalArgumentException | IllegalStateException e) {
                System.out.println("  [!] " + e.getMessage());
                if (!askYesNo("Try again?")) return false;
            }
        }
    }

    private static boolean askYesNo(final String prompt) {
        final String answer = Console.readLine(prompt + " (y/n)").trim().toLowerCase();
        return answer.equals("y");
    }

    @Override
    public String headline() {
        return "Create Flight Route (US073)";
    }
}
