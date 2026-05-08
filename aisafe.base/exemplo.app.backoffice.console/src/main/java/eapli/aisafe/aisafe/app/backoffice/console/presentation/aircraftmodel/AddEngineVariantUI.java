package eapli.aisafe.app.backoffice.console.presentation.aircraftmodel;

import eapli.aisafe.aircraftmodel.application.AddEngineVariantController;
import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI for US057 — Add Engine Variant to Aircraft Model.
 */
@SuppressWarnings("squid:S106")
public class AddEngineVariantUI extends AbstractUI {

    private final AddEngineVariantController controller = new AddEngineVariantController();

    @Override
    protected boolean doShow() {
        System.out.println("\nAvailable Aircraft Models:");
        controller.allAircraftModels().forEach(m -> System.out.println("  " + m));

        final String modelCode  = Console.readLine("Aircraft Model Code");
        final String engineCode = Console.readLine("Engine Model Code");

        System.out.println("Motorization types:");
        final MotorizationType[] types = MotorizationType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, types[i]);
        }
        final int idx = Console.readInteger("Select type (1-" + types.length + ")") - 1;

        try {
            controller.addVariant(modelCode, engineCode, types[idx]);
            System.out.println("Engine variant added successfully.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Add Engine Variant to Aircraft Model (US057)";
    }
}
