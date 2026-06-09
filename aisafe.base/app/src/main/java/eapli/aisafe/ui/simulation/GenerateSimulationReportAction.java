package eapli.aisafe.ui.simulation;

import eapli.framework.actions.Action;

public class GenerateSimulationReportAction implements Action {

    @Override
    public boolean execute() {
        return new GenerateSimulationReportUI().show();
    }
}
