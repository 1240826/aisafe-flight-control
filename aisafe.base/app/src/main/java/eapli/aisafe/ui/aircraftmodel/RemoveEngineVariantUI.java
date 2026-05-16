package eapli.aisafe.ui.aircraftmodel;

import eapli.aisafe.aircraftmodel.application.RemoveEngineVariantController;
import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftVariant;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * UI for US058 -Remove Engine Variant from Aircraft Model.
 */
@SuppressWarnings("squid:S106")
public class RemoveEngineVariantUI extends AbstractUI {

    private final RemoveEngineVariantController controller = new RemoveEngineVariantController();

    @Override
    protected boolean doShow() {

        // ── 1. Select Aircraft Model from numbered list ────────────────────────
        final List<AircraftModel> models = new ArrayList<>();
        controller.allAircraftModels().forEach(models::add);
        if (models.isEmpty()) {
            System.out.println("  [!] No aircraft models registered.");
            return false;
        }
        System.out.println("\nAvailable Aircraft Models:");
        for (int i = 0; i < models.size(); i++) {
            final AircraftModel m = models.get(i);
            System.out.printf("  %d. %s - %s (%d variant(s))%n",
                    i + 1, m.code(), m.name(), m.variants().size());
        }
        int modelIdx;
        do {
            modelIdx = Console.readInteger("Select aircraft model (1-" + models.size() + ")");
            if (modelIdx < 1 || modelIdx > models.size()) {
                System.out.println("  [!] Please enter a number between 1 and " + models.size() + ".");
            }
        } while (modelIdx < 1 || modelIdx > models.size());
        final AircraftModel selected = models.get(modelIdx - 1);

        // ── 2. Show variants of the selected model ─────────────────────────────
        final List<AircraftVariant> variants = new ArrayList<>(selected.variants());
        if (variants.isEmpty()) {
            System.out.println("  [!] This aircraft model has no engine variants.");
            return false;
        }
        if (variants.size() == 1) {
            System.out.println("  [!] This aircraft model has only one variant and it cannot be removed.");
            System.out.println("      (A model must always retain at least one engine variant.)");
            return false;
        }
        System.out.println("\nEngine variants of " + selected.code() + " - " + selected.name() + ":");
        for (int i = 0; i < variants.size(); i++) {
            final AircraftVariant v = variants.get(i);
            System.out.printf("  %d. %s (%s)%n", i + 1, v.engineModelCode(), v.motorizationType());
        }

        // ── 3. Select variant to remove ────────────────────────────────────────
        int variantIdx;
        do {
            variantIdx = Console.readInteger("Select variant to remove (1-" + variants.size() + ")");
            if (variantIdx < 1 || variantIdx > variants.size()) {
                System.out.println("  [!] Please enter a number between 1 and " + variants.size() + ".");
            }
        } while (variantIdx < 1 || variantIdx > variants.size());
        final String engineCode = variants.get(variantIdx - 1).engineModelCode().toString();

        // ── 4. Confirm and remove ──────────────────────────────────────────────
        System.out.printf("%n  You are about to remove variant '%s' from model '%s'. Confirm? (y/N) ",
                engineCode, selected.code());
        final String confirm = Console.readLine("").trim();
        if (!confirm.equalsIgnoreCase("y")) {
            System.out.println("  Operation cancelled.");
            return false;
        }

        try {
            controller.removeVariant(selected.code().toString(), engineCode);
            System.out.println("  >> Engine variant removed successfully.");
        } catch (final IllegalStateException | IllegalArgumentException e) {
            System.out.println("  [!] Could not remove engine variant: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Remove Engine Variant from Aircraft Model (US058)";
    }
}
