package eapli.aisafe.ui.aircraft;

import eapli.aisafe.aircraft.application.ListCompanyFleetController;
import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractListUI;
import eapli.framework.visitor.Visitor;

import java.util.ArrayList;
import java.util.List;

/**
 * UI for US072 — List Company Fleet (and sub-filters US072a, US072b, US072c, US072d).
 */
@SuppressWarnings("squid:S106")
public class ListCompanyFleetUI extends AbstractListUI<Aircraft> {

    private final ListCompanyFleetController controller = new ListCompanyFleetController();
    private String selectedCompanyIata = null;
    private int filterMode = 0; // 0=none, 1=model, 2=maker, 3=capacity, 4=age
    private String filterModel = null;
    private String filterMaker = null;
    private int filterNumber = 0; // used for capacity (US072c) and age (US072d)

    @Override
    protected boolean doShow() {
        // --- 1. Pick company ---
        final List<AirTransportCompany> companies = new ArrayList<>();
        try {
            controller.allCompanies().forEach(companies::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] Could not load companies: " + e.getMessage());
            return false;
        }

        System.out.println("\nAvailable Companies:");
        System.out.println("  0. Show all aircraft");
        for (int i = 0; i < companies.size(); i++) {
            final AirTransportCompany c = companies.get(i);
            System.out.printf("  %d. %s/%s - %s%n", i + 1, c.iata(), c.icao(), c.name());
        }

        int idx;
        do {
            idx = Console.readInteger("Select (0 for all, 1-" + companies.size() + " for company)");
            if (idx < 0 || idx > companies.size()) {
                System.out.println("  [!] Please enter a number between 0 and " + companies.size() + ".");
            }
        } while (idx < 0 || idx > companies.size());

        if (idx == 0) {
            selectedCompanyIata = null;
            filterMode = 0;
        } else {
            selectedCompanyIata = companies.get(idx - 1).iata().toString();

            // --- 2. Pick filter ---
            System.out.println("\nFilter by:");
            System.out.println("  0. No filter (show all)");
            System.out.println("  1. Model (US072a)");
            System.out.println("  2. Maker (US072b)");
            System.out.println("  3. Exact capacity (US072c)");
            System.out.println("  4. Age in years (US072d)");

            int filter;
            do {
                filter = Console.readInteger("Select filter (0-4)");
                if (filter < 0 || filter > 4) {
                    System.out.println("  [!] Please enter a number between 0 and 4.");
                }
            } while (filter < 0 || filter > 4);

            filterMode = filter;

            if (filterMode == 1) {
                filterModel = Console.readLine("Enter model code (e.g. A320, B738)").trim().toUpperCase();
            } else if (filterMode == 2) {
                filterMaker = Console.readLine("Enter maker name (e.g. Boeing, Airbus)").trim();
            } else if (filterMode == 3) {
                filterNumber = Console.readInteger("Enter exact capacity (number of passengers)");
            } else if (filterMode == 4) {
                filterNumber = Console.readInteger("Enter age of the aircraft (in years)");
            }
        }

        return super.doShow();
    }

    @Override
    public String headline() { return "Company Fleet (US072)"; }

    @Override
    protected String emptyMessage() { return "No aircraft found."; }

    @Override
    protected Iterable<Aircraft> elements() {
        try {
            if (selectedCompanyIata == null) {
                return controller.allActiveAircraft();
            }
            switch (filterMode) {
                case 1: return controller.fleetByModel(selectedCompanyIata, filterModel);
                case 2: return controller.fleetByMaker(selectedCompanyIata, filterMaker);
                case 3: return controller.fleetByCapacity(selectedCompanyIata, filterNumber);
                case 4: return controller.fleetByAge(selectedCompanyIata, filterNumber);
                default: return controller.fleetOfCompany(selectedCompanyIata);
            }
        } catch (final Exception e) {
            System.out.println("  [!] " + e.getMessage());
            return List.of();
        }
    }

    @Override
    protected Visitor<Aircraft> elementPrinter() {
        return a -> System.out.printf("  %-15s %-12s %-10s capacity=%-5d age=%-3d crew=%d%n",
                a.registrationNumber(), a.aircraftModelCode(),
                a.operationalStatus(), a.totalCapacity(), a.ageInYears(), a.numberOfFlightCrewMembers());
    }

    @Override
    protected String elementName() { return "Aircraft"; }

    @Override
    protected String listHeader() {
        return String.format("  %-15s %-12s %-10s %-11s %-6s %s",
                "REGISTRATION", "MODEL CODE", "STATUS", "CAPACITY", "AGE", "CREW");
    }
}