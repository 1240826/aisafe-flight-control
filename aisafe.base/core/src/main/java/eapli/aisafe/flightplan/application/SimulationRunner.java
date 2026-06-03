package eapli.aisafe.flightplan.application;

@FunctionalInterface
public interface SimulationRunner {
    String run(String jsonInput) throws SimulationRunnerException;
}
