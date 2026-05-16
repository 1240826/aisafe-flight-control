package eapli.aisafe.ui.enginemodel;

import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.aisafe.enginemodel.application.CreateEngineModelController;
import eapli.aisafe.enginemodel.domain.FuelType;
import eapli.aisafe.enginemodel.domain.Power;
import eapli.aisafe.enginemodel.domain.TSFC;
import eapli.aisafe.enginemodel.domain.Thrust;
import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * UI for US056 — Create Aircraft Engine Model.
 */
@SuppressWarnings("squid:S106")
public class CreateEngineModelUI extends AbstractUI {

    private final CreateEngineModelController controller = new CreateEngineModelController();

    @Override
    protected boolean doShow() {

        // ── 1. Manufacturer selection ──────────────────────────────────────────
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
        final String manufacturerName = manufacturers.get(mfgIdx - 1).name().toString();

        // ── 2. Engine Model Code ───────────────────────────────────────────────
        String code;
        do {
            code = Console.readLine("Engine Model Code (e.g. CFM56-7B27)").trim();
            if (code.isBlank()) {
                System.out.println("  [!] Code cannot be blank. Please try again.");
            }
        } while (code.isBlank());

        // ── 3. Engine Name — must contain at least one letter ─────────────────
        String name = null;
        while (name == null) {
            final String nameInput = Console.readLine("Engine Name (e.g. 'CFM56-7B27', 'Trent 970')").trim();
            if (nameInput.isBlank()) {
                System.out.println("  [!] Engine Name cannot be blank. Please try again.");
            } else if (!nameInput.matches(".*\\p{L}.*")) {
                System.out.println("  [!] Engine Name must contain at least one letter (e.g. 'CFM56'). Please try again.");
            } else {
                name = nameInput;
            }
        }

        // ── 4. Fuel type ───────────────────────────────────────────────────────
        System.out.println("Available fuel types:");
        for (int i = 0; i < FuelType.ALL.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, FuelType.ALL[i]);
        }
        int fuelIdx;
        do {
            fuelIdx = Console.readInteger("Select fuel type (1-" + FuelType.ALL.length + ")");
            if (fuelIdx < 1 || fuelIdx > FuelType.ALL.length) {
                System.out.println("  [!] Please enter a number between 1 and " + FuelType.ALL.length + ".");
            }
        } while (fuelIdx < 1 || fuelIdx > FuelType.ALL.length);
        final String fuel = FuelType.ALL[fuelIdx - 1];

        // ── 5. Motorization type ───────────────────────────────────────────────
        System.out.println("Motorization types:");
        final MotorizationType[] types = controller.motorizationTypes();
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
        final MotorizationType motorizationType = types[typeIdx - 1];

        // ── 6. Rated Power ─────────────────────────────────────────────────────
        System.out.println("\n-- Rated Power --");
        double powerVal;
        do {
            powerVal = Console.readDouble("  Power value (must be > 0)");
            if (powerVal <= 0) {
                System.out.println("  [!] Rated power must be greater than 0.");
            }
        } while (powerVal <= 0);

        System.out.println("  Power units:");
        for (int i = 0; i < Power.VALID_UNITS.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, Power.VALID_UNITS[i]);
        }
        int powerUnitIdx;
        do {
            powerUnitIdx = Console.readInteger("  Select power unit (1-" + Power.VALID_UNITS.length + ")");
            if (powerUnitIdx < 1 || powerUnitIdx > Power.VALID_UNITS.length) {
                System.out.println("  [!] Please enter a number between 1 and " + Power.VALID_UNITS.length + ".");
            }
        } while (powerUnitIdx < 1 || powerUnitIdx > Power.VALID_UNITS.length);
        final String powerUnit = Power.VALID_UNITS[powerUnitIdx - 1];

        // ── 7. Thrust unit (shared by static + cruise) ─────────────────────────
        System.out.println("\n-- Thrust --");
        System.out.println("  Thrust units:");
        for (int i = 0; i < Thrust.VALID_UNITS.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, Thrust.VALID_UNITS[i]);
        }
        int thrustUnitIdx;
        do {
            thrustUnitIdx = Console.readInteger("  Select thrust unit (1-" + Thrust.VALID_UNITS.length + ")");
            if (thrustUnitIdx < 1 || thrustUnitIdx > Thrust.VALID_UNITS.length) {
                System.out.println("  [!] Please enter a number between 1 and " + Thrust.VALID_UNITS.length + ".");
            }
        } while (thrustUnitIdx < 1 || thrustUnitIdx > Thrust.VALID_UNITS.length);
        final String tUnit = Thrust.VALID_UNITS[thrustUnitIdx - 1];

        // ── 8. Static thrust value ─────────────────────────────────────────────
        double stVal;
        do {
            stVal = Console.readDouble("  Static thrust value (take-off, " + tUnit + ", must be > 0)");
            if (stVal <= 0) {
                System.out.println("  [!] Static thrust must be greater than 0.");
            }
        } while (stVal <= 0);

        // ── 9. Cruise thrust value ─────────────────────────────────────────────
        double crVal;
        do {
            crVal = Console.readDouble("  Cruise thrust value (" + tUnit + ", must be > 0)");
            if (crVal <= 0) {
                System.out.println("  [!] Cruise thrust must be greater than 0.");
            }
        } while (crVal <= 0);

        // ── 10. TSFC ───────────────────────────────────────────────────────────
        System.out.println("\n-- TSFC (Thrust-Specific Fuel Consumption) --");
        double tsfcVal;
        do {
            tsfcVal = Console.readDouble("  TSFC value (must be > 0)");
            if (tsfcVal <= 0) {
                System.out.println("  [!] TSFC must be greater than 0.");
            }
        } while (tsfcVal <= 0);

        System.out.println("  TSFC units:");
        for (int i = 0; i < TSFC.VALID_UNITS.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, TSFC.VALID_UNITS[i]);
        }
        int tsfcUnitIdx;
        do {
            tsfcUnitIdx = Console.readInteger("  Select TSFC unit (1-" + TSFC.VALID_UNITS.length + ")");
            if (tsfcUnitIdx < 1 || tsfcUnitIdx > TSFC.VALID_UNITS.length) {
                System.out.println("  [!] Please enter a number between 1 and " + TSFC.VALID_UNITS.length + ".");
            }
        } while (tsfcUnitIdx < 1 || tsfcUnitIdx > TSFC.VALID_UNITS.length);
        final String tsfcUnit = TSFC.VALID_UNITS[tsfcUnitIdx - 1];

        // ── 11. Persist ────────────────────────────────────────────────────────
        try {
            controller.createEngineModel(code, name, manufacturerName, fuel, motorizationType,
                    powerVal, powerUnit, stVal, tUnit, crVal, tsfcVal, tsfcUnit);
            System.out.println("  >> Engine model created successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("  [!] Engine model code already exists.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Create Engine Model (US056)";
    }
}
