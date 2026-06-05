package eapli.aisafe.flightplan.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessBuilderSimulationRunnerTest {

    @Test
    void ensureConstructorStoresParameters() {
        final var runner = new ProcessBuilderSimulationRunner("/usr/bin/sim", 30);
        assertNotNull(runner);
    }

    @Test
    void ensureRunWithNonExistentExecutableThrows() {
        final var runner = new ProcessBuilderSimulationRunner(
                "/nonexistent/simulator", 5);
        assertThrows(SimulationRunnerException.class,
                () -> runner.run("{}"));
    }

    @Test
    void ensureNullJsonInputThrows() {
        final var runner = new ProcessBuilderSimulationRunner("echo", 5);
        assertThrows(SimulationRunnerException.class,
                () -> runner.run(null));
    }
}
