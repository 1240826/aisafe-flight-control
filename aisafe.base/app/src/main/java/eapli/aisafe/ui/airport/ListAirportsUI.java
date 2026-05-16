package eapli.aisafe.ui.airport;

import eapli.aisafe.airport.application.CreateAirportController;
import eapli.aisafe.airport.domain.Airport;
import eapli.framework.presentation.console.AbstractListUI;
import eapli.framework.visitor.Visitor;

/**
 * UI to list Airports.
 */
@SuppressWarnings("squid:S106")
public class ListAirportsUI extends AbstractListUI<Airport> {

    private final CreateAirportController controller = new CreateAirportController();

    @Override
    public String headline() { return "Airports"; }

    @Override
    protected String emptyMessage() { return "No airports registered."; }

    @Override
    protected Iterable<Airport> elements() { return controller.allAirports(); }

    @Override
    protected Visitor<Airport> elementPrinter() {
        return a -> System.out.printf("  %-5s %-6s %-35s %-20s %-15s%n",
                a.iata(), a.icao(), a.name(), a.city(), a.country());
    }

    @Override
    protected String elementName() { return "Airport"; }

    @Override
    protected String listHeader() {
        return String.format("  %-5s %-6s %-35s %-20s %-15s", "IATA", "ICAO", "NAME", "CITY", "COUNTRY");
    }
}
