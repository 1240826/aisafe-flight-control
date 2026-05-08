package eapli.aisafe.app.backoffice.console.presentation.manufacturer;

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
        final String name    = Console.readLine("Manufacturer Name");
        final String country = Console.readLine("Country");

        try {
            controller.registerManufacturer(name, country);
            System.out.println("Manufacturer registered successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Error: manufacturer name already exists.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Register Manufacturer";
    }
}
