package eapli.aisafe.ui.pilot;

import eapli.aisafe.pilot.application.RemovePilotController;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.List;

@SuppressWarnings("squid:S106")
public class RemovePilotUI extends AbstractUI {

    private final RemovePilotController controller = new RemovePilotController();

    @Override
    protected boolean doShow() {
        // ── List all pilots and pick one ─────────────────────────────────
        final List<Pilot> pilots;
        try {
            pilots = controller.allPilots();
        } catch (final IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
            return false;
        }
        if (pilots.isEmpty()) {
            System.out.println("  [!] No pilots registered.");
            return false;
        }

        Pilot selected = null;
        while (selected == null) {
            System.out.println("\n-- Pilots --");
            for (int i = 0; i < pilots.size(); i++) {
                final Pilot p = pilots.get(i);
                System.out.printf("  %d. %-10s %-6s %s%n",
                        i + 1, p.pilotId(), p.company(),
                        p.isActive() ? "ACTIVE" : "INACTIVE");
            }
            System.out.println("  0. Cancel");
            final int opt = Console.readInteger("Select pilot to deactivate (1-" + pilots.size() + ")");
            if (opt == 0) return false;
            if (opt < 1 || opt > pilots.size()) {
                System.out.println("  [!] Invalid option.");
                continue;
            }
            selected = pilots.get(opt - 1);
        }

        final PilotId pilotId = selected.pilotId();

        // ── Deactivate ───────────────────────────────────────────────────
        try {
            controller.deactivatePilot(pilotId);
            System.out.println("  >> Pilot " + pilotId + " deactivated successfully.");
        } catch (final IllegalArgumentException e) {
            System.out.println("  [!] " + e.getMessage());
        } catch (final IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Remove Pilot (US077)";
    }
}
