package eapli.aisafe.app.backoffice.console.presentation.aircraftmodel;

import eapli.aisafe.aircraftmodel.application.CreateAircraftModelController;
import eapli.aisafe.aircraftmodel.domain.AircraftType;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI for US055 — Create Aircraft Model.
 */
@SuppressWarnings("squid:S106")
public class CreateAircraftModelUI extends AbstractUI {

    private final CreateAircraftModelController controller = new CreateAircraftModelController();

    @Override
    protected boolean doShow() {
        final String code = Console.readLine("Model Code (e.g. B737-800)");
        final String name = Console.readLine("Model Name");
        final String mfr  = Console.readLine("Manufacturer Name");

        System.out.println("Aircraft types:");
        final AircraftType[] types = controller.aircraftTypes();
        for (int i = 0; i < types.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, types[i]);
        }
        final int typeIdx = Console.readInteger("Select type (1-" + types.length + ")") - 1;
        final AircraftType aircraftType = types[typeIdx];

        final int maxPax = Console.readInteger("Max Passengers (0 for N/A)");
        System.out.println("-- Weights (kg) --");
        final double ew   = Console.readDouble("  Empty Weight");
        final double mtow = Console.readDouble("  MTOW");
        final double mzfw = Console.readDouble("  MZFW");
        final double fuel = Console.readDouble("  Max Fuel Capacity");
        System.out.println("-- Performance --");
        final double ceil  = Console.readDouble("  Service Ceiling (m)");
        final double speed = Console.readDouble("  Cruise Speed (kt)");
        final double range = Console.readDouble("  Maximum Range (NM)");
        System.out.println("-- Aerodynamics --");
        final double wing = Console.readDouble("  Wing Area (m2)");
        final double drag = Console.readDouble("  Drag Coefficient (Cd)");
        final double lift = Console.readDouble("  Lift Coefficient (Cl)");

        try {
            controller.createAircraftModel(code, name, mfr, aircraftType,
                    maxPax == 0 ? null : maxPax,
                    ew, mtow, mzfw, fuel, ceil, speed, range, wing, drag, lift);
            System.out.println("Aircraft model created successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Error: model code already exists.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Create Aircraft Model (US055)";
    }
}
