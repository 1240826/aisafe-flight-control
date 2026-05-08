package eapli.aisafe.app.backoffice.console.presentation.aircraft;

import eapli.aisafe.aircraft.application.DecommissionAircraftController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI for US071 — Decommission Aircraft.
 */
@SuppressWarnings("squid:S106")
public class DecommissionAircraftUI extends AbstractUI {

    private final DecommissionAircraftController controller = new DecommissionAircraftController();

    @Override
    protected boolean doShow() {
        System.out.println("\nActive Aircraft:");
        controller.activeAircraft().forEach(a ->
                System.out.printf("  %-12s %-20s [crew=%d]%n",
                        a.registrationNumber(), a.aircraftModelCode(), a.numberOfFlightCrewMembers()));

        final String regNumber = Console.readLine("Registration Number");
        final String regCountry = Console.readLine("Registration Country");

        try {
            controller.decommissionAircraft(regNumber, regCountry);
            System.out.println("Aircraft decommissioned successfully.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Decommission Aircraft (US071)";
    }
}
