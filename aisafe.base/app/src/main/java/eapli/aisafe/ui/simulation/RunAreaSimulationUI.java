package eapli.aisafe.ui.simulation;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.simulation.application.RunAreaSimulationController;
import eapli.aisafe.simulation.domain.Simulation;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("squid:S106")
public class RunAreaSimulationUI extends AbstractUI {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RunAreaSimulationController controller =
            new RunAreaSimulationController();

    @Override
    protected boolean doShow() {
        final List<AirControlArea> areas = new ArrayList<>();
        try {
            controller.availableAreas().forEach(areas::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] Could not load areas: " + e.getMessage());
            return false;
        }

        if (areas.isEmpty()) {
            System.out.println("  [!] No Air Control Areas registered.");
            return false;
        }

        System.out.println("\nAvailable Air Control Areas:");
        for (int i = 0; i < areas.size(); i++) {
            final AirControlArea a = areas.get(i);
            System.out.printf("  %d. [%s] %s%n", i + 1, a.identity(), a.name());
        }

        int idx;
        do {
            idx = Console.readInteger("Select area (1-" + areas.size() + ", 0 to cancel)");
            if (idx == 0) return false;
            if (idx < 1 || idx > areas.size()) {
                System.out.println("  [!] Invalid option. Choose 1-" + areas.size() + ".");
            }
        } while (idx < 1 || idx > areas.size());

        final String areaCode = areas.get(idx - 1).identity().toString();

        LocalDateTime start = null;
        while (start == null) {
            try {
                start = LocalDateTime.parse(
                        Console.readLine("Simulation start (yyyy-MM-dd HH:mm)").trim(), FMT);
            } catch (final DateTimeParseException e) {
                System.out.println("  [!] Invalid format. Use yyyy-MM-dd HH:mm (e.g. 2027-06-01 08:00).");
            }
        }

        LocalDateTime end = null;
        while (end == null) {
            try {
                final LocalDateTime candidate = LocalDateTime.parse(
                        Console.readLine("Simulation end   (yyyy-MM-dd HH:mm)").trim(), FMT);
                if (!candidate.isAfter(start)) {
                    System.out.println("  [!] End must be after start (" + start + ").");
                } else {
                    end = candidate;
                }
            } catch (final DateTimeParseException e) {
                System.out.println("  [!] Invalid format. Use yyyy-MM-dd HH:mm (e.g. 2027-06-01 18:00).");
            }
        }

        double threshold;
        do {
            threshold = Console.readDouble("Safety threshold value (must be > 0)");
            if (threshold <= 0) System.out.println("  [!] Threshold must be greater than 0.");
        } while (threshold <= 0);

        String unit;
        do {
            unit = Console.readLine("Safety threshold unit (e.g. m/s, kt)").trim();
            if (unit.isBlank()) System.out.println("  [!] Unit cannot be blank.");
        } while (unit.isBlank());

        System.out.println("\n  >> Connecting to SCOMP simulator, please wait...");

        try {
            final Simulation sim = controller.runSimulation(areaCode, start, end, threshold, unit);
            System.out.println("  >> Simulation completed and saved successfully.");
            System.out.printf("     Area: %s  |  Time range: %s  |  Result: %s%n",
                    sim.areaCode(), sim.timeRange(), sim.validationResult());
            System.out.println("     Use 'Generate Simulation Report (US111)' to export the report to a file.");
        } catch (final IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Run Area Simulation (US111)";
    }
}
