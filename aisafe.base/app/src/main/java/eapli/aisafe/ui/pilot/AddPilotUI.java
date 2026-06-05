package eapli.aisafe.ui.pilot;

import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.pilot.application.AddPilotController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("squid:S106")
public class AddPilotUI extends AbstractUI {

    private final AddPilotController controller = new AddPilotController();

    @Override
    protected boolean doShow() {
        // ── License Number ───────────────────────────────────────────────
        String licenseNumber;
        while (true) {
            licenseNumber = Console.readLine("Pilot License Number (e.g. P12345)").trim().toUpperCase();
            if (licenseNumber.isBlank()) {
                System.out.println("  [!] License number cannot be blank.");
                continue;
            }
            try {
                eapli.aisafe.pilot.domain.PilotId.valueOf(licenseNumber);
                break;
            } catch (final IllegalArgumentException | IllegalStateException e) {
                System.out.println("  [!] " + e.getMessage());
            }
        }

        // ── Company (numbered list) ──────────────────────────────────────
        final List<AirTransportCompany> companies = new ArrayList<>();
        try {
            controller.allCompanies().forEach(companies::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
            return false;
        }
        if (companies.isEmpty()) {
            System.out.println("  [!] No companies registered. Create one first.");
            return false;
        }

        AirTransportCompany selectedCompany = null;
        while (selectedCompany == null) {
            System.out.println("\n-- Companies --");
            for (int i = 0; i < companies.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, companies.get(i));
            }
            final int opt = Console.readInteger("Select company (1-" + companies.size() + ")");
            if (opt < 1 || opt > companies.size()) {
                System.out.println("  [!] Invalid option.");
                continue;
            }
            selectedCompany = companies.get(opt - 1);
        }
        System.out.println("  >> Selected: " + selectedCompany);
        final CompanyIATA companyIata = selectedCompany.identity();

        // ── Certified Aircraft Models (numbered list, multi-select) ──────
        final List<AircraftModel> allModels = new ArrayList<>();
        try {
            controller.allAircraftModels().forEach(allModels::add);
        } catch (final IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
            return false;
        }
        if (allModels.isEmpty()) {
            System.out.println("  [!] No aircraft models registered. Create one first.");
            return false;
        }

        final Set<AircraftModelCode> certifiedModels = new HashSet<>();
        System.out.println("\n-- Certified Aircraft Models (select one or more) --");
        while (true) {
            for (int i = 0; i < allModels.size(); i++) {
                final String mark = certifiedModels.contains(allModels.get(i).identity()) ? " [X]" : " [ ]";
                System.out.printf("  %d.%s %s%n", i + 1, mark, allModels.get(i));
            }
            System.out.println("  0. Done");
            final int opt = Console.readInteger("Select model to toggle (0-" + allModels.size() + ")");
            if (opt == 0) break;
            if (opt < 1 || opt > allModels.size()) {
                System.out.println("  [!] Invalid option.");
                continue;
            }
            final AircraftModelCode code = allModels.get(opt - 1).identity();
            if (!certifiedModels.add(code)) {
                certifiedModels.remove(code);
            }
        }

        if (certifiedModels.isEmpty()) {
            System.out.println("  [!] At least one certified model is required.");
            return false;
        }

        // ── Certification Date ───────────────────────────────────────────
        LocalDate certDate = null;
        while (certDate == null) {
            try {
                certDate = LocalDate.parse(
                        Console.readLine("Certification Date (YYYY-MM-DD)"));
                if (certDate.isAfter(LocalDate.now())) {
                    System.out.println("  [!] Certification date cannot be in the future.");
                    certDate = null;
                }
            } catch (final DateTimeParseException e) {
                System.out.println("  [!] Invalid date format. Use YYYY-MM-DD.");
            }
        }

        // ── Save ─────────────────────────────────────────────────────────
        try {
            controller.addPilot(licenseNumber, companyIata, certifiedModels, certDate);
            System.out.println("  >> Pilot added successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("  [!] Pilot already exists.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Add Pilot (US075)";
    }
}
