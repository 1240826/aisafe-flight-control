package eapli.aisafe.ui.aircraftmodel;

import eapli.aisafe.aircraftmodel.application.AddEngineVariantController;
import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftVariant;
import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * UI for US057 — Add Engine Variant to Aircraft Model.
 */
@SuppressWarnings("squid:S106")
public class AddEngineVariantUI extends AbstractUI {

    private final AddEngineVariantController controller = new AddEngineVariantController();

    @Override
    protected boolean doShow() {

        // ── 1. Select Aircraft Model from numbered list ────────────────────────
        final List<AircraftModel> models = new ArrayList<>();
        controller.allAircraftModels().forEach(models::add);
        if (models.isEmpty()) {
            System.out.println("  [!] No aircraft models registered. Please create one first.");
            return false;
        }
        System.out.println("\nAvailable Aircraft Models:");
        for (int i = 0; i < models.size(); i++) {
            final AircraftModel m = models.get(i);
            final String variantInfo = m.variants().isEmpty()
                    ? "no variants yet"
                    : m.variants().size() + " variant(s), type: " + m.variants().get(0).motorizationType();
            System.out.printf("  %d. %s - %s [%s]%n", i + 1, m.code(), m.name(), variantInfo);
        }
        int modelIdx;
        do {
            modelIdx = Console.readInteger("Select aircraft model (1-" + models.size() + ")");
            if (modelIdx < 1 || modelIdx > models.size()) {
                System.out.println("  [!] Please enter a number between 1 and " + models.size() + ".");
            }
        } while (modelIdx < 1 || modelIdx > models.size());
        final AircraftModel selected = models.get(modelIdx - 1);

        // ── 2. Determine Motorization Type ────────────────────────────────────
        // If the model already has variants, the type is fixed by domain invariant.
        // Show it to the user and use it automatically — no selection needed.
        final MotorizationType motorizationType;
        if (!selected.variants().isEmpty()) {
            motorizationType = selected.variants().get(0).motorizationType();
            System.out.printf("%n  [i] Model '%s' already has variants with motorization type: %s%n",
                    selected.code(), motorizationType);
            System.out.println("      The new variant will also use " + motorizationType + ".");
            // Show existing variants
            System.out.println("  Existing variants:");
            for (final AircraftVariant v : selected.variants()) {
                System.out.println("    - " + v.engineModelCode() + " (" + v.motorizationType() + ")");
            }
        } else {
            // No variants yet — user chooses the motorization type for this model
            System.out.println("\nMotorization types:");
            final MotorizationType[] types = MotorizationType.values();
            for (int i = 0; i < types.length; i++) {
                System.out.printf("  %d. %s%n", i + 1, types[i]);
            }
            int typeIdx;
            do {
                typeIdx = Console.readInteger("Select type (1-" + types.length + ")");
                if (typeIdx < 1 || typeIdx > types.length) {
                    System.out.println("  [!] Please enter a number between 1 and " + types.length + ".");
                }
            } while (typeIdx < 1 || typeIdx > types.length);
            motorizationType = types[typeIdx - 1];
        }

        // ── 3. Select Engine Model from numbered list ──────────────────────────
        final List<EngineModel> engines = new ArrayList<>();
        controller.allEngineModels().forEach(engines::add);
        if (engines.isEmpty()) {
            System.out.println("  [!] No engine models registered. Please create one first.");
            return false;
        }
        System.out.println("\nAvailable Engine Models:");
        for (int i = 0; i < engines.size(); i++) {
            final EngineModel e = engines.get(i);
            System.out.printf("  %d. %s - %s (%s, %s)%n",
                    i + 1, e.identity(), e.engineName(), e.manufacturerName(), e.fuelType());
        }
        int engineIdx;
        do {
            engineIdx = Console.readInteger("Select engine model (1-" + engines.size() + ")");
            if (engineIdx < 1 || engineIdx > engines.size()) {
                System.out.println("  [!] Please enter a number between 1 and " + engines.size() + ".");
            }
        } while (engineIdx < 1 || engineIdx > engines.size());
        final String engineCode = engines.get(engineIdx - 1).identity().toString();

        // ── 4. Add variant ─────────────────────────────────────────────────────
        try {
            controller.addVariant(selected.code().toString(), engineCode, motorizationType);
            System.out.println("  >> Engine variant added successfully.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] Could not add engine variant: " + e.getMessage());
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("  [!] Could not add engine variant: duplicate or conflict.");
        }

        return false;
    }

    @Override
    public String headline() {
        return "Add Engine Variant to Aircraft Model (US057)";
    }
}
