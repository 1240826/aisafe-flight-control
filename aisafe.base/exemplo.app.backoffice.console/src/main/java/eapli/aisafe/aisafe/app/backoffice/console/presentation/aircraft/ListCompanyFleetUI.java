package eapli.aisafe.app.backoffice.console.presentation.aircraft;

import eapli.aisafe.aircraft.application.ListCompanyFleetController;
import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractListUI;
import eapli.framework.visitor.Visitor;

/**
 * UI for US072 — List Company Fleet.
 */
@SuppressWarnings("squid:S106")
public class ListCompanyFleetUI extends AbstractListUI<Aircraft> {

    private final ListCompanyFleetController controller = new ListCompanyFleetController();
    private String selectedCompanyIata = null;

    @Override
    protected boolean doShow() {
        System.out.println("\nAvailable Companies:");
        controller.allCompanies().forEach(c -> System.out.println("  " + c));
        selectedCompanyIata = Console.readLine("Company IATA Code (or ENTER for all active aircraft)");
        if (selectedCompanyIata.isBlank()) {
            selectedCompanyIata = null;
        }
        return super.doShow();
    }

    @Override
    public String headline() { return "Company Fleet (US072)"; }

    @Override
    protected String emptyMessage() { return "No aircraft found."; }

    @Override
    protected Iterable<Aircraft> elements() {
        if (selectedCompanyIata == null) {
            return controller.allActiveAircraft();
        }
        return controller.fleetOfCompany(selectedCompanyIata);
    }

    @Override
    protected Visitor<Aircraft> elementPrinter() {
        return a -> System.out.printf("  %-15s %-12s %-10s capacity=%-5d crew=%d%n",
                a.registrationNumber(), a.aircraftModelCode(),
                a.operationalStatus(), a.totalCapacity(), a.numberOfFlightCrewMembers());
    }

    @Override
    protected String elementName() { return "Aircraft"; }

    @Override
    protected String listHeader() {
        return String.format("  %-15s %-12s %-10s %-11s %s",
                "REGISTRATION", "MODEL CODE", "STATUS", "CAPACITY", "CREW");
    }
}
