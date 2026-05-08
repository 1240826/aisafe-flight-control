package eapli.aisafe.app.backoffice.console.presentation.aircraft;

import eapli.aisafe.aircraft.application.AddAircraftController;
import eapli.aisafe.aircraft.domain.SeatClass;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

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
        System.out.println("\nAvailable Aircraft Models:");
        controller.allAircraftModels().forEach(m -> System.out.println("  " + m));
        System.out.println("\nAvailable Companies:");
        controller.allCompanies().forEach(c -> System.out.println("  " + c));

        final String regNum     = Console.readLine("Registration Number (e.g. CS-TUI)");
        final String regCountry = Console.readLine("Registration Country");
        final String modelCode  = Console.readLine("Aircraft Model Code");
        final String companyIata = Console.readLine("Company IATA Code");
        final int crewMembers   = Console.readInteger("Number of Flight Crew Members");

        // Cabin configuration
        final List<SeatClass> seatClasses = new ArrayList<>();
        System.out.println("-- Cabin Configuration --");
        while (true) {
            final String className = Console.readLine("Seat Class Name (or ENTER to finish)");
            if (className.isBlank()) break;
            final int seats = Console.readInteger("Number of seats");
            seatClasses.add(new SeatClass(className, seats));
        }

        try {
            controller.addAircraft(regNum, regCountry, modelCode, companyIata,
                    crewMembers, seatClasses);
            System.out.println("Aircraft added successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Error: registration number already in use.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Add Aircraft to Company (US070)";
    }
}
