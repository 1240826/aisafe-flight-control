package eapli.aisafe.ui.flightplan;

import eapli.aisafe.flightplan.application.TestFlightPlanController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("squid:S106")
public class TestFlightPlanUI extends AbstractUI {

    private final TestFlightPlanController controller = new TestFlightPlanController();

    @Override
    protected boolean doShow() {
        final List<TestFlightPlanController.FlightPlanEntry> allEntries;
        try {
            allEntries = controller.allDraftEntries();
        } catch (final IllegalStateException e) {
            System.out.println("  [!] Could not load flight plans: " + e.getMessage());
            return false;
        }
        if (allEntries.isEmpty()) {
            System.out.println("  [!] No DRAFT flight plans available for testing.");
            return false;
        }

        System.out.println("\nAvailable Flight Plans for Testing:");
        System.out.printf("  %-3s %-12s %-12s %s%n", "#", "Flight", "Plan ID", "Route");
        System.out.println("  " + "-".repeat(55));
        int idx = 1;
        for (final var entry : allEntries) {
            final var flight = entry.flight();
            final var fp = entry.flightPlan();
            final String rn = flight.routeName() != null ? flight.routeName().toString() : "";
            System.out.printf("  %-3d %-12s %-12s %s%n",
                    idx, flight.identity(), fp.identity(),
                    "(" + rn + ")");
            idx++;
        }

        System.out.println("\nEnter numbers separated by space (e.g. '1 3 5') to test multiple flights as a scenario.");
        System.out.println("Enter a single number to test just that flight plan.");
        System.out.println("Enter 0 to cancel.");

        final String input = Console.readLine("Select flight plan(s)");
        if (input == null || input.trim().equals("0")) return false;

        final Set<Integer> selected = new LinkedHashSet<>();
        for (final var part : input.trim().split("\\s+")) {
            try {
                final int n = Integer.parseInt(part);
                if (n < 1 || n > allEntries.size()) {
                    System.out.println("  [!] Invalid number: " + n + " (must be 1-" + allEntries.size() + ")");
                    return false;
                }
                selected.add(n);
            } catch (final NumberFormatException e) {
                System.out.println("  [!] Invalid input: '" + part + "'");
                return false;
            }
        }

        if (selected.isEmpty()) return false;

        final List<TestFlightPlanController.FlightPlanEntry> entries = new ArrayList<>();
        for (final var n : selected) {
            entries.add(allEntries.get(n - 1));
        }

        if (entries.size() == 1) {
            final var entry = entries.get(0);
            try {
                final var result = controller.testFlightPlan(
                        entry.flight().identity().toString(),
                        entry.flightPlan().identity().toString());
                System.out.println("  >> " + result.message());
                if (result.reportContent() != null && !result.reportContent().isBlank()) {
                    System.out.println("  >> Report:");
                    System.out.println(result.reportContent());
                }
            } catch (final IllegalStateException | IllegalArgumentException e) {
                System.out.println("  [!] Could not test flight plan: " + e.getMessage());
            }
        } else {
            System.out.println("\nRunning scenario with " + entries.size() + " flight(s)...");
            try {
                final var result = controller.testScenario(entries);
                System.out.println("  >> " + result.message());
                if (result.reportContent() != null && !result.reportContent().isBlank()) {
                    System.out.println("  >> Report:");
                    System.out.println(result.reportContent());
                }
            } catch (final IllegalStateException | IllegalArgumentException e) {
                System.out.println("  [!] Could not run scenario: " + e.getMessage());
            }
        }

        return false;
    }

    @Override
    public String headline() {
        return "Test Flight Plan (US085)";
    }
}
