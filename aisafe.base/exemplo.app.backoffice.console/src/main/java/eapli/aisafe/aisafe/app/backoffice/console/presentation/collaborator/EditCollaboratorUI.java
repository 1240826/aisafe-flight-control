package eapli.aisafe.app.backoffice.console.presentation.collaborator;

import eapli.aisafe.collaborator.application.EditCollaboratorController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * UI for US063 — Edit Customer's Collaborator (Extra).
 */
@SuppressWarnings("squid:S106")
public class EditCollaboratorUI extends AbstractUI {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final EditCollaboratorController controller = new EditCollaboratorController();

    @Override
    protected boolean doShow() {
        System.out.println("\nActive Collaborators:");
        controller.activeCollaborators().forEach(c ->
                System.out.printf("  [%d] %s — %s%n", c.id(), c.name(), c.position()));

        final long id = Console.readLong("Collaborator ID to edit");

        final String newName     = Console.readLine("New Name     (ENTER to keep current)");
        final String newPosition = Console.readLine("New Position (ENTER to keep current)");
        final String newClearStr = Console.readLine("New Security Clearance Expiry Date yyyy-MM-dd (ENTER to skip)");
        final String newAssStr   = Console.readLine("New Skills Assessment Date yyyy-MM-dd          (ENTER to skip)");

        final LocalDate newClearance  = newClearStr.isBlank()  ? null : LocalDate.parse(newClearStr, DATE_FMT);
        final LocalDate newAssessment = newAssStr.isBlank()    ? null : LocalDate.parse(newAssStr, DATE_FMT);

        try {
            controller.editCollaborator(id,
                    newName.isBlank()     ? null : newName,
                    newPosition.isBlank() ? null : newPosition,
                    newClearance, newAssessment);
            System.out.println("Collaborator updated successfully.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Edit Collaborator (US063)";
    }
}
