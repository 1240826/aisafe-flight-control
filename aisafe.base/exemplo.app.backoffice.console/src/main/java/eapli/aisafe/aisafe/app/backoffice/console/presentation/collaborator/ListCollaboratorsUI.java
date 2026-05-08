package eapli.aisafe.app.backoffice.console.presentation.collaborator;

import eapli.aisafe.collaborator.application.ListCollaboratorsController;
import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractListUI;
import eapli.framework.visitor.Visitor;

/**
 * UI for US062 — List Customer's Collaborators.
 */
@SuppressWarnings("squid:S106")
public class ListCollaboratorsUI extends AbstractListUI<Collaborator> {

    private final ListCollaboratorsController controller = new ListCollaboratorsController();
    private String selectedCompanyIata = null;

    @Override
    protected boolean doShow() {
        System.out.println("\nAvailable Companies:");
        controller.allCompanies().forEach(c -> System.out.println("  " + c));
        selectedCompanyIata = Console.readLine("Company IATA Code (or ENTER to list all active)");
        if (selectedCompanyIata.isBlank()) {
            selectedCompanyIata = null;
        }
        return super.doShow();
    }

    @Override
    public String headline() { return "Collaborators (US062)"; }

    @Override
    protected String emptyMessage() { return "No collaborators found."; }

    @Override
    protected Iterable<Collaborator> elements() {
        if (selectedCompanyIata == null) {
            return controller.allActiveCollaborators();
        }
        return controller.collaboratorsOfCompany(selectedCompanyIata);
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
