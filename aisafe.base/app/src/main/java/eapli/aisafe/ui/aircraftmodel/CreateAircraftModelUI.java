package eapli.aisafe.ui.aircraftmodel;

import eapli.aisafe.aircraftmodel.application.CreateAircraftModelController;
import eapli.aisafe.aircraftmodel.domain.AircraftType;
import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * UI for US055 — Create Aircraft Model.
 */
@SuppressWarnings("squid:S106")
public class CreateAircraftModelUI extends AbstractUI {

    private final CreateAircraftModelController controller = new CreateAircraftModelController();

    @Override
    protected boolean doShow() {

        // --- Manufacturer selection from registered list (same pattern as engine model) ---
        final List<Manufacturer> manufacturers = new ArrayList<>();
        controller.allManufacturers().forEach(manufacturers::add);
        if (manufacturers.isEmpty()) {
            System.out.println("  [!] No manufacturers registered. Please register a manufacturer first.");
            return false;
        }
        System.out.println("\nManufacturers:");
        for (int i = 0; i < manufacturers.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, manufacturers.get(i).name());
        }
        int mfgIdx;
        do {
            mfgIdx = Console.readInteger("Select manufacturer (1-" + manufacturers.size() + ")");
            if (mfgIdx < 1 || mfgIdx > manufacturers.size()) {
                System.out.println("  [!] Please enter a number between 1 and " + manufacturers.size() + ".");
            }
        } while (mfgIdx < 1 || mfgIdx > manufacturers.size());
        final String mfr = manufacturers.get(mfgIdx - 1).name().toString();

        // --- Model Code — non-blank, auto uppercase ---
        String code;
        do {
            code = Console.readLine("Model Code (e.g. B737-800)").trim().toUpperCase();
            if (code.isBlank()) {
                System.out.println("  [!] Model Code cannot be blank. Please try again.");
            }
        } while (code.isBlank());

        // --- Model Name — non-blank ---
        String name;
        do {
            name = Console.readLine("Model Name").trim();
            if (name.isBlank()) {
                System.out.println("  [!] Model Name cannot be blank. Please try again.");
            }
        } while (name.isBlank());

        // --- Aircraft Type selection ---
        System.out.println("Aircraft types:");
        final AircraftType[] types = controller.aircraftTypes();
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
        final AircraftType aircraftType = types[typeIdx - 1];

        // --- Max Passengers — must be >= 0 (0 = cargo/N/A) ---
        int maxPax;
        do {
            maxPax = Console.readInteger("Max Passengers (0 for N/A)");
            if (maxPax < 0) {
                System.out.println("  [!] Max Passengers cannot be negative.");
            }
        } while (maxPax < 0);

        // --- Weights (kg): MTOW > MZFW > emptyWeight > 0 ---
        System.out.println("-- Weights (kg) -- Note: MTOW > MZFW > Empty Weight");
        double ew;
        do {
            ew = Console.readDouble("  Empty Weight (kg, must be > 0)");
            if (ew <= 0) System.out.println("  [!] Empty Weight must be greater than 0.");
        } while (ew <= 0);

        double mzfw;
        do {
            mzfw = Console.readDouble("  MZFW - Max Zero Fuel Weight (kg, must be > Empty Weight " + ew + ")");
            if (mzfw <= 0) {
                System.out.println("  [!] MZFW must be greater than 0.");
            } else if (mzfw <= ew) {
                System.out.println("  [!] MZFW (" + mzfw + ") must be greater than Empty Weight (" + ew + ").");
            }
        } while (mzfw <= 0 || mzfw <= ew);

        double mtow;
        do {
            mtow = Console.readDouble("  MTOW - Max Take-Off Weight (kg, must be > MZFW " + mzfw + ")");
            if (mtow <= 0) {
                System.out.println("  [!] MTOW must be greater than 0.");
            } else if (mtow <= mzfw) {
                System.out.println("  [!] MTOW (" + mtow + ") must be greater than MZFW (" + mzfw + ").");
            }
        } while (mtow <= 0 || mtow <= mzfw);

        double fuel;
        do {
            fuel = Console.readDouble("  Max Fuel Capacity (kg, must be > 0)");
            if (fuel <= 0) System.out.println("  [!] Max Fuel Capacity must be greater than 0.");
        } while (fuel <= 0);

        // --- Performance ---
        System.out.println("-- Performance --");
        double ceil;
        do {
            ceil = Console.readDouble("  Service Ceiling (m, must be > 0)");
            if (ceil <= 0) System.out.println("  [!] Service Ceiling must be greater than 0.");
        } while (ceil <= 0);

        double speed;
        do {
            speed = Console.readDouble("  Cruise Speed (kt, must be > 0)");
            if (speed <= 0) System.out.println("  [!] Cruise Speed must be greater than 0.");
        } while (speed <= 0);

        double range;
        do {
            range = Console.readDouble("  Maximum Range (NM, must be > 0)");
            if (range <= 0) System.out.println("  [!] Maximum Range must be greater than 0.");
        } while (range <= 0);

        // --- Aerodynamics ---
        System.out.println("-- Aerodynamics --");
        double wing;
        do {
            wing = Console.readDouble("  Wing Area (m2, must be > 0)");
            if (wing <= 0) System.out.println("  [!] Wing Area must be greater than 0.");
        } while (wing <= 0);

        double drag;
        do {
            drag = Console.readDouble("  Drag Coefficient (Cd, must be > 0)");
            if (drag <= 0) System.out.println("  [!] Drag Coefficient must be greater than 0.");
        } while (drag <= 0);

        double lift;
        do {
            lift = Console.readDouble("  Lift Coefficient (Cl, must be > 0)");
            if (lift <= 0) System.out.println("  [!] Lift Coefficient must be greater than 0.");
        } while (lift <= 0);

        try {
            controller.createAircraftModel(code, name, mfr, aircraftType,
                    maxPax == 0 ? null : maxPax,
                    ew, mtow, mzfw, fuel, ceil, speed, range, wing, drag, lift);
            System.out.println("  >> Aircraft model created successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("  [!] Could not create aircraft model: model code already exists.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] Could not create aircraft model: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Create Aircraft Model (US055)";
    }
}
