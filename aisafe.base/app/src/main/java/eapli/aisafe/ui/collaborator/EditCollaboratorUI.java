package eapli.aisafe.ui.collaborator;

import eapli.aisafe.collaborator.application.EditCollaboratorController;
import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * UI for US063 - Edit Customer's Collaborator (Extra).
 */
@SuppressWarnings("squid:S106")
public class EditCollaboratorUI extends AbstractUI {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final EditCollaboratorController controller = new EditCollaboratorController();

    @Override
    protected boolean doShow() {
        final List<Collaborator> collaborators = new ArrayList<>();
        controller.activeCollaborators().forEach(collaborators::add);

        if (collaborators.isEmpty()) {
            System.out.println("  [!] No active collaborators available.");
            return false;
        }

        System.out.println("\nActive Collaborators:");
        System.out.println("  0. Cancel");
        for (int i = 0; i < collaborators.size(); i++) {
            final Collaborator c = collaborators.get(i);
            System.out.printf("  %d. %s - %s%n", i + 1, c.name(), c.position());
        }

        int idx;
        do {
            idx = Console.readInteger("Select collaborator to edit (0 to cancel, 1-" + collaborators.size() + ")");
            if (idx < 0 || idx > collaborators.size()) {
                System.out.println("  [!] Please enter a number between 0 and " + collaborators.size() + ".");
            }
        } while (idx < 0 || idx > collaborators.size());

        if (idx == 0) {
            System.out.println("  Cancelled.");
            return false;
        }

        final Collaborator selected = collaborators.get(idx - 1);

        // Optional fields — blank means keep current value / clear phone
        // If a value is provided it must contain at least one letter (name/position)
        String newName = null;
        while (newName == null) {
            final String input = Console.readLine("New Name (ENTER to keep current)");
            if (input.isBlank()) {
                newName = "";   // sentinel: keep current
            } else if (!input.matches(".*\\p{L}.*")) {
                System.out.println("  [!] Name must contain at least one letter.");
            } else {
                newName = input;
            }
        }

        String newPosition = null;
        while (newPosition == null) {
            final String input = Console.readLine("New Position (ENTER to keep current)");
            if (input.isBlank()) {
                newPosition = "";   // sentinel: keep current
            } else if (!input.matches(".*\\p{L}.*")) {
                System.out.println("  [!] Position must contain at least one letter.");
            } else {
                newPosition = input;
            }
        }

        final String newPhone = Console.readLine("New Phone (ENTER to keep/clear)");

        // Security Clearance Expiry Date - blank to skip; if provided, parse with retry
        LocalDate newClearance = null;
        boolean clearanceDone = false;
        while (!clearanceDone) {
            String newClearStr = Console.readLine("New Security Clearance Expiry Date yyyy-MM-dd (ENTER to skip)").trim();
            if (newClearStr.isBlank()) {
                clearanceDone = true;
            } else {
                try {
                    newClearance = LocalDate.parse(newClearStr, DATE_FMT);
                    clearanceDone = true;
                } catch (DateTimeParseException e) {
                    System.out.println("  [!] Invalid date format. Please use yyyy-MM-dd (e.g. 2027-12-31), or press ENTER to skip.");
                }
            }
        }

        // Skills Assessment Date - blank to skip; if provided, parse with retry
        LocalDate newAssessment = null;
        boolean assessmentDone = false;
        while (!assessmentDone) {
            String newAssStr = Console.readLine("New Skills Assessment Date yyyy-MM-dd          (ENTER to skip)").trim();
            if (newAssStr.isBlank()) {
                assessmentDone = true;
            } else {
                try {
                    newAssessment = LocalDate.parse(newAssStr, DATE_FMT);
                    assessmentDone = true;
                } catch (DateTimeParseException e) {
                    System.out.println("  [!] Invalid date format. Please use yyyy-MM-dd (e.g. 2027-12-31), or press ENTER to skip.");
                }
            }
        }

        try {
            controller.editCollaborator(selected.id(),
                    newName.isBlank()     ? null : newName,
                    newPosition.isBlank() ? null : newPosition,
                    newPhone,
                    newClearance, newAssessment);
            System.out.println("  >> Collaborator updated successfully.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Edit Collaborator (US063)";
    }
}
