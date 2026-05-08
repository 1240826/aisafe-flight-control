package eapli.aisafe.app.backoffice.console.presentation.enginemodel;

import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.aisafe.enginemodel.application.CreateEngineModelController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI for US056 — Create Aircraft Engine Model.
 */
@SuppressWarnings("squid:S106")
public class CreateEngineModelUI extends AbstractUI {

    private final CreateEngineModelController controller = new CreateEngineModelController();

    @Override
    protected boolean doShow() {
        final String code = Console.readLine("Engine Model Code (e.g. CFM56-7B27)");
        final String name = Console.readLine("Engine Name");
        final String fuel = Console.readLine("Fuel Type (e.g. Jet-A1)");

        System.out.println("Motorization types:");
        final MotorizationType[] types = controller.motorizationTypes();
        for (int i = 0; i < types.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, types[i]);
        }
        final int typeIdx = Console.readInteger("Select type (1-" + types.length + ")") - 1;
        final MotorizationType motorizationType = types[typeIdx];

        final double powerVal  = Console.readDouble("Rated Power value");
        final String powerUnit = Console.readLine("Power unit (e.g. kW, hp)");
        final double stVal  = Console.readDouble("Static thrust value");
        final String tUnit  = Console.readLine("Thrust unit (e.g. kN, lbf)");
        final double crVal  = Console.readDouble("Cruise thrust value");
        final double tsfcVal  = Console.readDouble("TSFC value");
        final String tsfcUnit = Console.readLine("TSFC unit (e.g. g/kN/s)");

        try {
            controller.createEngineModel(code, name, fuel, motorizationType,
                    powerVal, powerUnit, stVal, tUnit, crVal, tsfcVal, tsfcUnit);
            System.out.println("Engine model created successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Error: engine model code already exists.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Create Engine Model (US056)";
    }
}
