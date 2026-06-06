package eapli.aisafe.flightplan.application;

public interface SimulationRunner {
    String run(String jsonInput) throws SimulationRunnerException;

    default String run(final String jsonInput, final String weatherFilePath) throws SimulationRunnerException {
        return run(jsonInput);
    }
}
