package eapli.aisafe.ui.aircraft;

import eapli.aisafe.aircraft.application.DecommissionAircraftController;
import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * UI for US071 — Decommission Aircraft.
 */
@SuppressWarnings("squid:S106")
public class DecommissionAircraftUI extends AbstractUI {

    private final DecommissionAircraftController controller = new DecommissionAircraftController();

    @Override
    protected boolean doShow() {

        // --- Load active aircraft ---
        final List<Aircraft> activeAircraft = new ArrayList<>();
        try {
            controller.activeAircraft().forEach(activeAircraft::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] Could not load active aircraft: " + e.getMessage());
            return false;
        }
        if (activeAircraft.isEmpty()) {
            System.out.println("  [!] No active aircraft registered.");
            return false;
        }

        System.out.println("\nActive Aircraft:");
        for (int i = 0; i < activeAircraft.size(); i++) {
            final Aircraft a = activeAircraft.get(i);
            System.out.printf("  %d. %s (%s) - %s - %s%n",
                    i + 1,
                    a.registrationNumber().number(),
                    a.registrationNumber().registrationCountry(),
                    a.aircraftModelCode(),
                    a.companyId());
        }

        int idx;
        do {
            idx = Console.readInteger("Select aircraft to decommission (1-" + activeAircraft.size() + ")");
            if (idx < 1 || idx > activeAircraft.size()) {
                System.out.println("  [!] Please enter a number between 1 and " + activeAircraft.size() + ".");
            }
        } while (idx < 1 || idx > activeAircraft.size());

        final Aircraft selected = activeAircraft.get(idx - 1);
        final String regNumber = selected.registrationNumber().number();
        final String regCountry = selected.registrationNumber().registrationCountry();

        try {
            controller.decommissionAircraft(regNumber, regCountry);
            System.out.println("  >> Aircraft decommissioned successfully.");
        } catch (final IllegalStateException | IllegalArgumentException e) {
            System.out.println("  [!] Could not decommission aircraft: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Decommission Aircraft (US071)";
    }
}
