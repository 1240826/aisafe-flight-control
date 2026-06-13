package eapli.aisafe.simulation.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationTest {

    private static AreaCode areaCode() { return new AreaCode("LPPC"); }

    private static SimulationTimeRange timeRange() {
        return new SimulationTimeRange(
                java.time.LocalDateTime.of(2026, 6, 1, 0, 0),
                java.time.LocalDateTime.of(2026, 6, 1, 23, 59));
    }

    private static SafetyThreshold threshold() {
        return new SafetyThreshold(10.0, "meters");
    }

    private static SimulationReport report() {
        return new SimulationReport("/tmp/sim.txt", "Simulation output content");
    }

    @Test
    void ensureValidSimulationCanBeCreated() {
        final var sim = new Simulation(areaCode(), timeRange(), threshold(), report());
        assertEquals(areaCode(), sim.areaCode());
        assertEquals(timeRange(), sim.timeRange());
        assertEquals(threshold(), sim.safetyThreshold());
        assertEquals(report(), sim.report());
        assertEquals(ValidationResult.PENDING, sim.validationResult());
    }

    @Test
    void ensureDefaultValidationResultIsPending() {
        final var sim = new Simulation(areaCode(), timeRange(), threshold(), report());
        assertEquals(ValidationResult.PENDING, sim.validationResult());
    }

    @Test
    void ensureRecordValidationResultChangesStatus() {
        final var sim = new Simulation(areaCode(), timeRange(), threshold(), report());
        sim.recordValidationResult(ValidationResult.PASSED);
        assertEquals(ValidationResult.PASSED, sim.validationResult());
    }

    @Test
    void ensureRecordValidationResultToFailed() {
        final var sim = new Simulation(areaCode(), timeRange(), threshold(), report());
        sim.recordValidationResult(ValidationResult.FAILED);
        assertEquals(ValidationResult.FAILED, sim.validationResult());
    }

    @Test
    void ensureRecordValidationResultRejectsNull() {
        final var sim = new Simulation(areaCode(), timeRange(), threshold(), report());
        assertThrows(Exception.class, () -> sim.recordValidationResult(null));
    }

    @Test
    void ensureConstructorRejectsNullAreaCode() {
        assertThrows(Exception.class,
                () -> new Simulation(null, timeRange(), threshold(), report()));
    }

    @Test
    void ensureConstructorRejectsNullTimeRange() {
        assertThrows(Exception.class,
                () -> new Simulation(areaCode(), null, threshold(), report()));
    }

    @Test
    void ensureConstructorRejectsNullSafetyThreshold() {
        assertThrows(Exception.class,
                () -> new Simulation(areaCode(), timeRange(), null, report()));
    }

    @Test
    void ensureConstructorRejectsNullReport() {
        assertThrows(Exception.class,
                () -> new Simulation(areaCode(), timeRange(), threshold(), null));
    }

    @Test
    void ensureToStringContainsAreaCode() {
        final var sim = new Simulation(areaCode(), timeRange(), threshold(), report());
        assertTrue(sim.toString().contains("LPPC"));
    }
}
