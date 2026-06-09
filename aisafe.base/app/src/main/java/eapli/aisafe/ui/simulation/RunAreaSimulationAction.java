package eapli.aisafe.ui.simulation;

import eapli.framework.actions.Action;

public class RunAreaSimulationAction implements Action {

    @Override
    public boolean execute() {
        return new RunAreaSimulationUI().show();
    }
}
