package eapli.aisafe.app.backoffice.console.presentation.aircontrolarea;

import eapli.aisafe.aircontrolarea.application.RegisterAirControlAreaController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI for US050 — Register Air Control Area.
 */
@SuppressWarnings("squid:S106")
public class RegisterAirControlAreaUI extends AbstractUI {

    private final RegisterAirControlAreaController controller =
            new RegisterAirControlAreaController();

    @Override
    protected boolean doShow() {
        final String code = Console.readLine("Area Code (e.g. ACC-LIS)");
        final String name = Console.readLine("Area Name");
        final double minLat = Console.readDouble("Min Latitude");
        final double maxLat = Console.readDouble("Max Latitude");
        final double minLon = Console.readDouble("Min Longitude");
        final double maxLon = Console.readDouble("Max Longitude");

        try {
            controller.registerAirControlArea(code, name, minLat, maxLat, minLon, maxLon);
            System.out.println("Air Control Area registered successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Error: area code already exists.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Register Air Control Area (US050)";
    }
}
