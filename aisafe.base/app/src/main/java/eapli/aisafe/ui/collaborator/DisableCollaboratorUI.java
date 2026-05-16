package eapli.aisafe.ui.collaborator;

import eapli.aisafe.collaborator.application.DisableCollaboratorController;
import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * UI for US064 - Disable Customer's Collaborator (Extra).
 */
@SuppressWarnings("squid:S106")
public class DisableCollaboratorUI extends AbstractUI {

    private final DisableCollaboratorController controller = new DisableCollaboratorController();

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
            idx = Console.readInteger("Select collaborator to disable (0 to cancel, 1-" + collaborators.size() + ")");
            if (idx < 0 || idx > collaborators.size()) {
                System.out.println("  [!] Please enter a number between 0 and " + collaborators.size() + ".");
            }
        } while (idx < 0 || idx > collaborators.size());

        if (idx == 0) {
            System.out.println("  Cancelled.");
            return false;
        }

        final Collaborator selected = collaborators.get(idx - 1);

        try {
            controller.disableCollaborator(selected.id());
            System.out.println("  >> Collaborator disabled successfully.");
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
