package eapli.aisafe.ui.aircontrolarea;

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

        // Area Code — non-blank
        String code;
        do {
            code = Console.readLine("Area Code (e.g. ACC-LIS)").trim();
            if (code.isBlank()) {
                System.out.println("  [!] Area Code cannot be blank.");
            }
        } while (code.isBlank());

        // Area Name — must contain at least one letter
        String name = null;
        while (name == null) {
            final String nameInput = Console.readLine("Area Name (e.g. 'Lisbon ACC')").trim();
            if (nameInput.isBlank()) {
                System.out.println("  [!] Area Name cannot be blank.");
            } else if (!nameInput.matches(".*\\p{L}.*")) {
                System.out.println("  [!] Area Name must contain at least one letter.");
            } else {
                name = nameInput;
            }
        }

        // Min Latitude — -90 to 90
        double minLat;
        do {
            minLat = Console.readDouble("Min Latitude (-90 to 90)");
            if (minLat < -90 || minLat > 90) {
                System.out.println("  [!] Min Latitude must be between -90 and 90.");
            }
        } while (minLat < -90 || minLat > 90);

        // Max Latitude — -90 to 90, AND must be strictly greater than minLat
        double maxLat;
        do {
            maxLat = Console.readDouble("Max Latitude (-90 to 90)");
            if (maxLat < -90 || maxLat > 90) {
                System.out.println("  [!] Max Latitude must be between -90 and 90.");
            } else if (maxLat <= minLat) {
                System.out.println("  [!] Max Latitude must be greater than Min Latitude (" + minLat + ").");
            }
        } while (maxLat < -90 || maxLat > 90 || maxLat <= minLat);

        // Min Longitude — -180 to 180
        double minLon;
        do {
            minLon = Console.readDouble("Min Longitude (-180 to 180)");
            if (minLon < -180 || minLon > 180) {
                System.out.println("  [!] Min Longitude must be between -180 and 180.");
            }
        } while (minLon < -180 || minLon > 180);

        // Max Longitude — -180 to 180, AND must be strictly greater than minLon
        double maxLon;
        do {
            maxLon = Console.readDouble("Max Longitude (-180 to 180)");
            if (maxLon < -180 || maxLon > 180) {
                System.out.println("  [!] Max Longitude must be between -180 and 180.");
            } else if (maxLon <= minLon) {
                System.out.println("  [!] Max Longitude must be greater than Min Longitude (" + minLon + ").");
            }
        } while (maxLon < -180 || maxLon > 180 || maxLon <= minLon);

        try {
            controller.registerAirControlArea(code, name, minLat, maxLat, minLon, maxLon);
            System.out.println("  >> Air Control Area registered successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("  [!] Area code already exists.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Register Air Control Area (US050)";
    }
}
