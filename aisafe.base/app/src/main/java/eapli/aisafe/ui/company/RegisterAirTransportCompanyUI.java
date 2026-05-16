package eapli.aisafe.ui.company;

import eapli.aisafe.company.application.RegisterAirTransportCompanyController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI for US060 — Register Air Transport Company.
 */
@SuppressWarnings("squid:S106")
public class RegisterAirTransportCompanyUI extends AbstractUI {

    private final RegisterAirTransportCompanyController controller =
            new RegisterAirTransportCompanyController();

    @Override
    protected boolean doShow() {

        // Company IATA Code — exactly 2 uppercase letters
        String iata;
        do {
            iata = Console.readLine("IATA Code (2 uppercase letters, e.g. TP)").trim().toUpperCase();
            if (!iata.matches("[A-Z]{2}")) {
                System.out.println("  [!] Company IATA Code must be exactly 2 uppercase letters (e.g. TP).");
            }
        } while (!iata.matches("[A-Z]{2}"));

        // Company ICAO Code — 2 or 3 uppercase letters
        String icao;
        do {
            icao = Console.readLine("ICAO Code (2-3 uppercase letters, e.g. TAP)").trim().toUpperCase();
            if (!icao.matches("[A-Z]{2,3}")) {
                System.out.println("  [!] Company ICAO Code must be 2 or 3 uppercase letters (e.g. TAP).");
            }
        } while (!icao.matches("[A-Z]{2,3}"));

        // Company Name — must contain at least one letter (cannot be digits/symbols only)
        String name = null;
        while (name == null) {
            final String nameInput = Console.readLine("Company Name (e.g. 'TAP Air Portugal')").trim();
            if (nameInput.isBlank()) {
                System.out.println("  [!] Company Name cannot be blank.");
            } else if (!nameInput.matches(".*\\p{L}.*")) {
                System.out.println("  [!] Company Name must contain at least one letter.");
            } else {
                name = nameInput;
            }
        }

        try {
            controller.registerCompany(iata, icao, name);
            System.out.println("  >> Air Transport Company registered successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Error: IATA/ICAO code or name already exists.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Register Air Transport Company (US060)";
    }
}
