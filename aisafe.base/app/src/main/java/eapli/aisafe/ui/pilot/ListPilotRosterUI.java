package eapli.aisafe.ui.pilot;

import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.pilot.application.ListPilotRosterController;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("squid:S106")
public class ListPilotRosterUI extends AbstractUI {

    private final ListPilotRosterController controller = new ListPilotRosterController();

    @Override
    protected boolean doShow() {
        // ── Company (numbered list) ──────────────────────────────────────
        final List<AirTransportCompany> companies = new ArrayList<>();
        try {
            controller.allCompanies().forEach(companies::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
            return false;
        }
        if (companies.isEmpty()) {
            System.out.println("  [!] No companies registered.");
            return false;
        }

        AirTransportCompany selected = null;
        while (selected == null) {
            System.out.println("\n-- Companies --");
            for (int i = 0; i < companies.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, companies.get(i));
            }
            System.out.println("  0. Cancel");
            final int opt = Console.readInteger("Select company (1-" + companies.size() + ")");
            if (opt == 0) return false;
            if (opt < 1 || opt > companies.size()) {
                System.out.println("  [!] Invalid option.");
                continue;
            }
            selected = companies.get(opt - 1);
        }
        final CompanyIATA companyIata = selected.identity();

        // ── List pilots ──────────────────────────────────────────────────
        final List<Pilot> pilots = new ArrayList<>();
        try {
            controller.listCompanyPilots(companyIata).forEach(pilots::add);
        } catch (final Exception e) {
            System.out.println("  [!] Could not load pilots: " + e.getMessage());
            return false;
        }

        if (pilots.isEmpty()) {
            System.out.println("  No pilots found for " + selected);
            return false;
        }

        System.out.println("\nPilot Roster for " + selected + ":");
        System.out.printf("  %-12s %-10s %-8s %s%n", "License", "Company", "Status", "Models");
        for (final Pilot p : pilots) {
            System.out.printf("  %-12s %-10s %-8s %s%n",
                    p.pilotId(),
                    p.company(),
                    p.isActive() ? "ACTIVE" : "INACTIVE",
                    p.certifiedModels());
        }

        return false;
    }

    @Override
    public String headline() {
        return "List Pilot Roster (US076)";
    }
}
