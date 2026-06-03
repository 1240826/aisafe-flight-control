package eapli.aisafe.flightplan.application;

public class SimulationRunnerException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SimulationRunnerException(final String message) {
        super(message);
    }

    public SimulationRunnerException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
