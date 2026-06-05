package eapli.aisafe.ui.collaborator;

import eapli.aisafe.collaborator.application.ListCollaboratorsController;
import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractListUI;
import eapli.framework.visitor.Visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UI for US062 - List Customer's Collaborators.
 */
@SuppressWarnings("squid:S106")
public class ListCollaboratorsUI extends AbstractListUI<Collaborator> {

    private final ListCollaboratorsController controller = new ListCollaboratorsController();
    private String selectedCompanyIata = null;

    @Override
    protected boolean doShow() {
        final List<AirTransportCompany> companies = new ArrayList<>();
        try {
            controller.allCompanies().forEach(companies::add);
        } catch (final Exception e) {
            System.out.println("  [!] Could not load companies: " + e.getMessage());
            return false;
        }

        System.out.println("\nFilter by company:");
        System.out.println("  0. All collaborators");
        for (int i = 0; i < companies.size(); i++) {
            final AirTransportCompany c = companies.get(i);
            System.out.printf("  %d. %s/%s - %s%n", i + 1, c.iata(), c.icao(), c.name());
        }

        int idx;
        do {
            idx = Console.readInteger("Select (0-" + companies.size() + ")");
            if (idx < 0 || idx > companies.size()) {
                System.out.println("  [!] Please enter a number between 0 and " + companies.size() + ".");
            }
        } while (idx < 0 || idx > companies.size());

        if (idx == 0) {
            selectedCompanyIata = null;
        } else {
            selectedCompanyIata = companies.get(idx - 1).iata().toString();
        }

        return super.doShow();
    }

    @Override
    public String headline() { return "Collaborators (US062)"; }

    @Override
    protected String emptyMessage() { return "No collaborators found."; }

    @Override
    protected Iterable<Collaborator> elements() {
        try {
            if (selectedCompanyIata == null) {
                return controller.allActiveCollaborators();
            }
            return controller.collaboratorsOfCompany(selectedCompanyIata);
        } catch (final Exception e) {
            System.out.println("  [!] " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    protected Visitor<Collaborator> elementPrinter() {
        return c -> System.out.printf("  %-5d %-30s %-20s %-10s%n",
                c.id(), c.name(), c.position(),
                c.isActive() ? "ACTIVE" : "DISABLED");
    }

    @Override
    protected String elementName() { return "Collaborator"; }

    @Override
    protected String listHeader() {
        return String.format("  %-5s %-30s %-20s %-10s", "ID", "NAME", "POSITION", "STATUS");
    }
}
