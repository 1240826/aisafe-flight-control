package eapli.aisafe.ui.simulation;

import eapli.aisafe.simulation.application.SaveSimulationController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * UI for saving a SCOMP simulation result.
 * The operator provides the path to the SCOMP output file; the system reads
 * and stores the content verbatim.
 */
@SuppressWarnings("squid:S106")
public class SaveSimulationUI extends AbstractUI {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SaveSimulationController controller = new SaveSimulationController();

    @Override
    protected boolean doShow() {
        // --- area code ---
        String areaCode;
        do {
            areaCode = Console.readLine("Air Control Area Code").trim();
            if (areaCode.isBlank()) {
                System.out.println("  [!] Area code cannot be blank. Please try again.");
            }
        } while (areaCode.isBlank());

        // --- simulation start ---
        LocalDateTime start = null;
        while (start == null) {
            try {
                String raw = Console.readLine("Simulation start (yyyy-MM-dd HH:mm)").trim();
                start = LocalDateTime.parse(raw, FMT);
            } catch (DateTimeParseException e) {
                System.out.println("  [!] Invalid format. Use yyyy-MM-dd HH:mm (e.g. 2027-06-01 14:30).");
            }
        }

        // --- simulation end ---
        LocalDateTime end = null;
        while (end == null) {
            try {
                String raw = Console.readLine("Simulation end   (yyyy-MM-dd HH:mm)").trim();
                LocalDateTime candidate = LocalDateTime.parse(raw, FMT);
                if (!candidate.isAfter(start)) {
                    System.out.println("  [!] Simulation end must be after start (" + start + "). Please try again.");
                } else {
                    end = candidate;
                }
            } catch (DateTimeParseException e) {
                System.out.println("  [!] Invalid format. Use yyyy-MM-dd HH:mm (e.g. 2027-06-01 14:30).");
            }
        }

        // --- threshold ---
        double threshold;
        do {
            threshold = Console.readDouble("Safety threshold value (must be > 0)");
            if (threshold <= 0) {
                System.out.println("  [!] Threshold must be greater than 0.");
            }
        } while (threshold <= 0);

        // --- unit ---
        String unit;
        do {
            unit = Console.readLine("Safety threshold unit (e.g. m/s, kt)").trim();
            if (unit.isBlank()) {
                System.out.println("  [!] Unit cannot be blank. Please try again.");
            }
        } while (unit.isBlank());

        // --- file path ---
        String filePath = null;
        String content = null;
        while (content == null) {
            filePath = Console.readLine("Path to SCOMP output file (.txt)").trim();
            try {
                content = java.nio.file.Files.readString(java.nio.file.Path.of(filePath));
            } catch (java.io.IOException e) {
                System.out.println("  [!] Could not read file '" + filePath + "'. Check the path and try again.");
            }
        }

        try {
            controller.saveSimulation(areaCode, start, end, threshold, unit, filePath, content);
            System.out.println("  >> Simulation saved successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Error: concurrency or integrity violation.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Save Simulation Result (SCOMP output)";
    }
}
