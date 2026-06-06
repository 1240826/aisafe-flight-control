package eapli.aisafe.ui.flightplan;

import eapli.aisafe.flightplan.application.ImportFlightPlanController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("squid:S106")
public class ImportFlightPlanUI extends AbstractUI {

    private final ImportFlightPlanController controller = new ImportFlightPlanController();

    private static final java.util.Set<String> CURATED_FILES = java.util.Set.of(
            "valid_lis_cdg.flightplan",
            "valid_opo_waw.flightplan",
            "valid_lis_opo.flightplan",
            "invalid_sem_zero_fuel.flightplan",
            "valid_demo_conflict_a.flightplan",
            "valid_demo_conflict_b.flightplan",
            "valid_demo_regular_usa_lis.flightplan",
            "valid_demo_charter_lis_mad.flightplan");

    @Override
    protected boolean doShow() {
        // ── Find .flightplan files (curated subset) ─────────────────────
        final List<Path> foundFiles = new ArrayList<>();
        final List<String> searchPaths = List.of(
                "flightplans",
                "../aisafe.dsl/src/main/resources/examples",
                "aisafe.dsl/src/main/resources/examples",
                ".");
        for (final String sp : searchPaths) {
            final Path dir = Paths.get(sp);
            if (Files.isDirectory(dir)) {
                try (Stream<Path> stream = Files.list(dir)) {
                    stream.filter(p -> p.toString().endsWith(".flightplan"))
                            .filter(p -> CURATED_FILES.contains(p.getFileName().toString()))
                            .sorted()
                            .forEach(foundFiles::add);
                } catch (final IOException e) {
                    // skip
                }
            }
        }

        Path selectedFile = null;
        if (!foundFiles.isEmpty()) {
            System.out.println("\n-- Available .flightplan files --");
            for (int i = 0; i < foundFiles.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, foundFiles.get(i).getFileName());
            }
            System.out.println("  0. Enter path manually\n");
            while (selectedFile == null) {
                final int opt = Console.readInteger("Select file (1-" + foundFiles.size() + ")");
                if (opt == 0) break;
                if (opt < 1 || opt > foundFiles.size()) {
                    System.out.println("  [!] Invalid option.");
                    continue;
                }
                selectedFile = foundFiles.get(opt - 1);
            }
        }

        // ── Manual path (if none selected from list) ─────────────────────
        if (selectedFile == null) {
            while (true) {
                final String pathStr = Console.readLine("Path to .flightplan file");
                if (pathStr == null || pathStr.isBlank()) {
                    System.out.println("  [!] No file path provided.");
                    return false;
                }
                selectedFile = Paths.get(pathStr);
                if (!Files.exists(selectedFile) || !Files.isReadable(selectedFile)) {
                    System.out.println("  [!] File not found or not readable: " + pathStr);
                    selectedFile = null;
                    continue;
                }
                break;
            }
        }

        final String dslContent;
        final String dslSource = selectedFile.getFileName().toString();
        try {
            dslContent = Files.readString(selectedFile);
        } catch (final IOException e) {
            System.out.println("  [!] Error reading file: " + e.getMessage());
            return false;
        }

        // ── Flight Plan ID: extracted from DSL content ───────────────────
        final String flightPlanId;
        try {
            flightPlanId = controller.extractFlightDesignator(dslContent);
            System.out.println("  Flight Plan ID: " + flightPlanId
                    + " (extracted from " + dslSource + ")");
        } catch (final Exception e) {
            System.out.println("  [!] Could not extract flight ID from DSL: " + e.getMessage());
            return false;
        }

        // ── Import + validate ────────────────────────────────────────────
        ImportFlightPlanController.DslValidationResult result;
        try {
            result = controller.importFlightPlan(dslContent, dslSource, flightPlanId);
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
            return false;
        }

        System.out.println("\n--- Validating DSL ---");
        if (!result.lexicalPassed()) {
            System.out.println("  [LEXER] FAILED");
            result.lexicalErrors().forEach(e -> System.out.println("    " + e));
        } else {
            System.out.println("  [LEXER] PASSED");
        }

        if (!result.syntacticPassed()) {
            System.out.println("  [PARSER] FAILED");
            result.syntacticErrors().forEach(e -> System.out.println("    " + e));
        } else {
            System.out.println("  [PARSER] PASSED");
        }

        if (result.lexicalPassed() && result.syntacticPassed()) {
            if (!result.semanticPassed()) {
                System.out.println("  [SEMANTIC] FAILED");
                result.semanticErrors().forEach(e -> System.out.println("    " + e));
            } else {
                System.out.println("  [SEMANTIC] PASSED");
            }
        }

        if (result.allPassed()) {
            System.out.println("\n  >> VALIDATION PASSED");
            System.out.println("\n  Flight Plan Summary:");
            System.out.println(result.summary());
            System.out.println("  Flight Plan " + result.flightPlan().identity()
                    + " created with status " + result.flightPlan().status());
        } else {
            System.out.println("\n  >> VALIDATION FAILED");
            System.out.println("  Flight plan was NOT created.");
        }

        return false;
    }

    @Override
    public String headline() {
        return "Import Flight Plan from DSL file (US081/121)";
    }
}
