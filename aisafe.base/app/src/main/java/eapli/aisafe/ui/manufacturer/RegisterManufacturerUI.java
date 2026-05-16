package eapli.aisafe.ui.manufacturer;

import eapli.aisafe.manufacturer.application.RegisterManufacturerController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI to register a Manufacturer.
 */
@SuppressWarnings("squid:S106")
public class RegisterManufacturerUI extends AbstractUI {

    private final RegisterManufacturerController controller = new RegisterManufacturerController();

    @Override
    protected boolean doShow() {
        // --- name ---
        String name;
        do {
            name = Console.readLine("Manufacturer Name").trim();
            if (name.isBlank()) {
                System.out.println("  [!] Name cannot be blank. Please try again.");
            }
        } while (name.isBlank());

        // --- country ---
        String country = null;
        while (country == null) {
            final String countryInput = Console.readLine("Country (e.g. 'Portugal', 'United States')").trim();
            if (countryInput.isBlank()) {
                System.out.println("  [!] Country cannot be blank. Please try again.");
            } else if (!countryInput.matches(".*\\p{L}.*")) {
                System.out.println("  [!] Country must contain at least one letter. Please try again.");
            } else {
                country = countryInput;
            }
        }

        try {
            controller.registerManufacturer(name, country);
            System.out.println("  >> Manufacturer registered successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Error: manufacturer name already exists.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Register Manufacturer";
    }
}
