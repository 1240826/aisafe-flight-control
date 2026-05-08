package eapli.aisafe.app.backoffice.console.presentation.company;

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
        final String iata = Console.readLine("IATA Code (2 letters, e.g. TP)");
        final String icao = Console.readLine("ICAO Code (2-3 letters, e.g. TAP)");
        final String name = Console.readLine("Company Name");

        try {
            controller.registerCompany(iata, icao, name);
            System.out.println("Air Transport Company registered successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Error: IATA/ICAO code or name already exists.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Register Air Transport Company (US060)";
    }
}
