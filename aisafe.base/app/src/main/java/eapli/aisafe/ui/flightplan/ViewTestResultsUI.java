package eapli.aisafe.ui.flightplan;

import eapli.aisafe.flightplan.application.TestFlightPlanController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

@SuppressWarnings("squid:S106")
public class ViewTestResultsUI extends AbstractUI {

    private final TestFlightPlanController controller = new TestFlightPlanController();

    @Override
    protected boolean doShow() {
        final var entries = controller.allTestedEntries();
        if (entries.isEmpty()) {
            System.out.println("  [!] No test results available yet. Test a flight plan first.");
            return false;
        }

        System.out.println("\n--- Test Results ---");
        int idx = 1;
        for (final var entry : entries) {
            final var flight = entry.flight();
            final var fp = entry.flightPlan();
            final var status = fp.status();
            final var icon = status == eapli.aisafe.flightplan.domain.FlightPlanStatus.TEST_PASSED ? "PASS" : "FAIL";
            final String testedAt = fp.lastTestedAt() != null ? fp.lastTestedAt().toString() : "?";
            System.out.printf("  %d. %s / %s  [%s]  %s%n",
                    idx, flight.identity(), fp.identity(), icon, testedAt);
            idx++;
        }

        final int choice = Console.readInteger("\nSelect a flight plan to view its report (0 to cancel)");
        if (choice < 1 || choice > entries.size()) return false;

        final var selected = entries.get(choice - 1);
        final var fp = selected.flightPlan();
        System.out.println("\n============================================");
        System.out.println("  Report for " + selected.flight().identity() + " / " + fp.identity());
        System.out.println("  Status: " + fp.status());
        System.out.println("  Tested at: " + (fp.lastTestedAt() != null ? fp.lastTestedAt() : "?"));
        System.out.println("============================================");
        if (fp.reportContent() != null) {
            System.out.println(fp.reportContent());
        } else {
            System.out.println("  (no report content)");
        }
        System.out.println("============================================");
        Console.readLine("\nPress Enter to continue...");
        return false;
    }

    @Override
    public String headline() {
        return "View Test Results";
    }
}
