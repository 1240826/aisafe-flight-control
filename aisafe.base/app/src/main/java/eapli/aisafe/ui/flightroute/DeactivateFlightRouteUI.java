package eapli.aisafe.ui.flightroute;

import eapli.aisafe.flightroute.application.DeleteFlightRouteController;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("squid:S106")
public class DeactivateFlightRouteUI extends AbstractUI {

    private final DeleteFlightRouteController controller = new DeleteFlightRouteController();

    @Override
    protected boolean doShow() {
        final List<FlightRoute> activeRoutes = new ArrayList<>();
        try {
            controller.activeRoutes().forEach(activeRoutes::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] Could not load routes: " + e.getMessage());
            return false;
        }

        if (activeRoutes.isEmpty()) {
            System.out.println("  [!] No active routes to deactivate.");
            return false;
        }

        System.out.println("\nActive Routes:");
        for (int i = 0; i < activeRoutes.size(); i++) {
            final FlightRoute r = activeRoutes.get(i);
            System.out.printf("  %d. %s  %s -> %s  [%s]%n",
                    i + 1, r.routeName(), r.origin(), r.destination(), r.companyIATA());
        }

        int idx;
        do {
            idx = Console.readInteger("Select route to deactivate (1-" + activeRoutes.size() + ")");
            if (idx == 0) {
                return false;
            }
            if (idx < 1 || idx > activeRoutes.size()) {
                System.out.println("  [!] Invalid option.");
            }
        } while (idx < 1 || idx > activeRoutes.size());

        final String routeName = activeRoutes.get(idx - 1).routeName().toString();

        LocalDate deactivationDate = null;
        while (deactivationDate == null) {
            try {
                deactivationDate = LocalDate.parse(
                        Console.readLine("Deactivation Date (YYYY-MM-DD)"));
            } catch (final DateTimeParseException e) {
                System.out.println("  [!] Invalid date format. Use YYYY-MM-DD.");
            }
        }

        try {
            controller.deactivateRoute(routeName, deactivationDate);
            System.out.println("  >> Route deactivated successfully from " + deactivationDate);
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Deactivate Flight Route (US074)";
    }
}
