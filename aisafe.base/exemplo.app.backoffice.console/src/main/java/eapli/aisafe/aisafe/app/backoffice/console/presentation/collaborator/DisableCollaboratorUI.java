package eapli.aisafe.app.backoffice.console.presentation.collaborator;

import eapli.aisafe.collaborator.application.DisableCollaboratorController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI for US064 — Disable Customer's Collaborator (Extra).
 */
@SuppressWarnings("squid:S106")
public class DisableCollaboratorUI extends AbstractUI {

    private final DisableCollaboratorController controller = new DisableCollaboratorController();

    @Override
    protected boolean doShow() {
        System.out.println("\nActive Collaborators:");
        controller.activeCollaborators().forEach(c ->
                System.out.printf("  [%d] %s — %s%n", c.id(), c.name(), c.position()));

        final long id = Console.readLong("Collaborator ID to disable");

        try {
            controller.disableCollaborator(id);
            System.out.println("Collaborator disabled successfully.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Disable Collaborator (US064)";
    }
}
