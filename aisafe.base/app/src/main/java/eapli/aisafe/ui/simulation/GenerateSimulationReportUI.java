package eapli.aisafe.ui.simulation;

import eapli.aisafe.simulation.application.GenerateSimulationReportController;
import eapli.aisafe.simulation.domain.Simulation;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@SuppressWarnings("squid:S106")
public class GenerateSimulationReportUI extends AbstractUI {

    private final GenerateSimulationReportController controller =
            new GenerateSimulationReportController();

    @Override
    protected boolean doShow() {
        final List<Simulation> simulations = new ArrayList<>();
        try {
            controller.allSimulations().forEach(simulations::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] Could not load simulations: " + e.getMessage());
            return false;
        }

        if (simulations.isEmpty()) {
            System.out.println("  [!] No simulations available.");
            return false;
        }

        System.out.println("\nAvailable Simulations:");
        for (int i = 0; i < simulations.size(); i++) {
            final Simulation s = simulations.get(i);
            System.out.printf("  %d. Area: %s  %s  [%s]%n",
                    i + 1, s.areaCode(), s.timeRange(), s.validationResult());
        }

        int idx;
        do {
            idx = Console.readInteger("Select simulation (1-" + simulations.size() + ", 0 to cancel)");
            if (idx == 0) {
                return false;
            }
            if (idx < 1 || idx > simulations.size()) {
                System.out.println("  [!] Invalid option. Please choose a number between 1 and " + simulations.size() + ".");
            }
        } while (idx < 1 || idx > simulations.size());

        final String areaCode = simulations.get(idx - 1).areaCode().toString();

        try {
            final String reportPath = controller.generateReport(areaCode);
            System.out.println("  >> Report generated successfully: " + reportPath);
        } catch (final NoSuchElementException e) {
            System.out.println("  [!] No simulation found for the selected area: " + e.getMessage());
        } catch (final IllegalArgumentException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Generate Simulation Report";
    }
}
