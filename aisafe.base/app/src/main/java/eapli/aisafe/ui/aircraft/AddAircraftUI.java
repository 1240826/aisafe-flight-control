package eapli.aisafe.ui.aircraft;

import eapli.aisafe.aircraft.application.AddAircraftController;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * UI for US070 — Add Aircraft to Air Transport Company.
 */
@SuppressWarnings("squid:S106")
public class AddAircraftUI extends AbstractUI {

    private final AddAircraftController controller = new AddAircraftController();

    @Override
    protected boolean doShow() {

        // --- Aircraft Model selection ---
        final List<AircraftModel> models = new ArrayList<>();
        try {
            controller.allAircraftModels().forEach(models::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] Could not load aircraft models: " + e.getMessage());
            return false;
        }
        if (models.isEmpty()) {
            System.out.println("  [!] No aircraft models available. Please register some first.");
            return false;
        }
        System.out.println("\nAvailable Aircraft Models:");
        for (int i = 0; i < models.size(); i++) {
            final AircraftModel m = models.get(i);
            final String maxPax = m.maxPassengers() != null ? m.maxPassengers() + " pax" : "n/a pax";
            System.out.printf("  %d. %s - %s (%s, %s, %s)%n",
                    i + 1, m.code(), m.name(), m.manufacturerName(), m.aircraftType(), maxPax);
        }
        int modelIdx;
        do {
            modelIdx = Console.readInteger("Select aircraft model (1-" + models.size() + ")");
            if (modelIdx < 1 || modelIdx > models.size()) {
                System.out.println("  [!] Please enter a number between 1 and " + models.size() + ".");
            }
        } while (modelIdx < 1 || modelIdx > models.size());
        final String modelCode = models.get(modelIdx - 1).code().toString();

        // --- Company selection ---
        final List<AirTransportCompany> companies = new ArrayList<>();
        try {
            controller.allCompanies().forEach(companies::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] Could not load companies: " + e.getMessage());
            return false;
        }
        if (companies.isEmpty()) {
            System.out.println("  [!] No companies available. Please register some first.");
            return false;
        }
        System.out.println("\nAvailable Companies:");
        for (int i = 0; i < companies.size(); i++) {
            final AirTransportCompany c = companies.get(i);
            System.out.printf("  %d. %s/%s - %s%n",
                    i + 1, c.iata(), c.icao(), c.name());
        }
        int companyIdx;
        do {
            companyIdx = Console.readInteger("Select company (1-" + companies.size() + ")");
            if (companyIdx < 1 || companyIdx > companies.size()) {
                System.out.println("  [!] Please enter a number between 1 and " + companies.size() + ".");
            }
        } while (companyIdx < 1 || companyIdx > companies.size());
        final String companyIata = companies.get(companyIdx - 1).iata().toString();

        // --- Registration Number — must start with a letter; letters/digits/hyphens only ---
        String regNum = null;
        while (regNum == null) {
            final String input = Console.readLine("Registration Number (e.g. CS-TUI, G-BNWA, N12345)").trim().toUpperCase();
            if (input.isBlank()) {
                System.out.println("  [!] Registration Number cannot be blank.");
            } else if (!input.matches("[A-Z][A-Z0-9\\-]{0,7}")) {
                System.out.println("  [!] Registration Number must start with a letter and contain only "
                        + "uppercase letters, digits, and hyphens (e.g. 'CS-TUI').");
            } else {
                regNum = input;
            }
        }

        // --- Registration Country — must contain at least one letter ---
        String regCountry = null;
        while (regCountry == null) {
            final String input = Console.readLine("Registration Country (e.g. 'Portugal')").trim();
            if (input.isBlank()) {
                System.out.println("  [!] Registration Country cannot be blank.");
            } else if (!input.matches(".*\\p{L}.*")) {
                System.out.println("  [!] Registration Country must contain at least one letter.");
            } else {
                regCountry = input;
            }
        }

        // --- Number of Flight Crew Members — must be >= 1 ---
        int crewMembers;
        do {
            crewMembers = Console.readInteger("Number of Flight Crew Members (min. 1)");
            if (crewMembers < 1) {
                System.out.println("  [!] Crew members must be at least 1.");
            }
        } while (crewMembers < 1);

        // --- Registration Date ---
        LocalDate registrationDate = null;
        while (registrationDate == null) {
            try {
                registrationDate = LocalDate.parse(
                        Console.readLine("Registration Date (YYYY-MM-DD, must not be in the future)").trim());
                if (registrationDate.isAfter(LocalDate.now())) {
                    System.out.println("  [!] Registration date cannot be in the future.");
                    registrationDate = null;
                }
            } catch (final DateTimeParseException e) {
                System.out.println("  [!] Invalid date format. Use YYYY-MM-DD (e.g. 2018-03-15).");
            }
        }

        // --- Cabin configuration ---
        final List<SeatClass> seatClasses = new ArrayList<>();
        System.out.println("-- Cabin Configuration --");
        while (true) {
            final String className = Console.readLine("Seat Class Name (or ENTER to finish)").trim();
            if (className.isBlank()) break;

            int seats;
            do {
                seats = Console.readInteger("Number of seats (min. 1)");
                if (seats < 1) {
                    System.out.println("  [!] Seat count must be at least 1.");
                }
            } while (seats < 1);

            seatClasses.add(new SeatClass(className, seats));
        }

        try {
            controller.addAircraft(regNum, regCountry, modelCode, companyIata,
                    crewMembers, seatClasses, registrationDate);
            System.out.println("  >> Aircraft added successfully.");
        } catch (final IllegalStateException | IllegalArgumentException
                       | IntegrityViolationException | ConcurrencyException e) {
            System.out.println("  [!] Could not add aircraft: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Add Aircraft to Company (US070)";
    }
}
