package eapli.aisafe.app.backoffice.console.presentation.aircraftmodel;

import eapli.aisafe.aircraftmodel.application.RemoveEngineVariantController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI for US058 — Remove Engine Variant from Aircraft Model (Extra).
 */
@SuppressWarnings("squid:S106")
public class RemoveEngineVariantUI extends AbstractUI {

    private final RemoveEngineVariantController controller = new RemoveEngineVariantController();

    @Override
    protected boolean doShow() {
        System.out.println("\nAvailable Aircraft Models:");
        controller.allAircraftModels().forEach(m -> {
            System.out.println("  " + m);
            m.variants().forEach(v -> System.out.println("    -> " + v));
        });

        final String modelCode  = Console.readLine("Aircraft Model Code");
        final String engineCode = Console.readLine("Engine Model Code to remove");

        try {
            controller.removeVariant(modelCode, engineCode);
            System.out.println("Engine variant removed successfully.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Remove Engine Variant from Aircraft Model (US058)";
    }
}
