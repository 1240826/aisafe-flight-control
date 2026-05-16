package eapli.aisafe.simulation.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Simulation aggregate root and its value objects.
 * Covers: SimulationTimeRange, SafetyThreshold, SimulationReport, ValidationResult.
 */
class SimulationTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static final LocalDateTime START =
            LocalDateTime.of(2026, 6, 1, 8, 0);
    private static final LocalDateTime END =
            LocalDateTime.of(2026, 6, 1, 10, 0);

    private static SimulationTimeRange validTimeRange() {
        return new SimulationTimeRange(START, END);
    }

    private static SafetyThreshold validThreshold() {
        return new SafetyThreshold(50.0, "kt");
    }

    private static SimulationReport validReport() {
        return new SimulationReport("/scomp/output/sim_001.txt",
                "SCOMP simulation output content here.");
    }

    private static Simulation validSimulation() {
        return new Simulation(
                new AreaCode("LPPC"),
                validTimeRange(),
                validThreshold(),
                validReport());
    }

    // ── Simulation happy path ─────────────────────────────────────────────────

    @Test
    void ensureValidSimulationCanBeCreated() {
        final var sim = validSimulation();
        assertNotNull(sim);
        assertEquals("LPPC", sim.areaCode().toString());
        assertEquals(ValidationResult.PENDING, sim.validationResult(),
                "New simulation should default to PENDING");
    }

    @Test
    void ensureValidationResultCanBeRecorded() {
        final var sim = validSimulation();
        sim.recordValidationResult(ValidationResult.PASSED);
        assertEquals(ValidationResult.PASSED, sim.validationResult());
    }

    @Test
    void ensureValidationResultCanBeSetToFailed() {
        final var sim = validSimulation();
        sim.recordValidationResult(ValidationResult.FAILED);
        assertEquals(ValidationResult.FAILED, sim.validationResult());
    }

    // ── Simulation invariants ─────────────────────────────────────────────────

    @Test
    void ensureNullAreaCodeIsRejected() {
        assertThrows(Exception.class,
                () -> new Simulation(null, validTimeRange(), validThreshold(), validReport()));
    }

    @Test
    void ensureNullTimeRangeIsRejected() {
        assertThrows(Exception.class,
                () -> new Simulation(new AreaCode("LPPC"), null, validThreshold(), validReport()));
    }

    @Test
    void ensureNullSafetyThresholdIsRejected() {
        assertThrows(Exception.class,
                () -> new Simulation(new AreaCode("LPPC"), validTimeRange(), null, validReport()));
    }

    @Test
    void ensureNullReportIsRejected() {
        assertThrows(Exception.class,
                () -> new Simulation(new AreaCode("LPPC"), validTimeRange(), validThreshold(), null));
    }

    // ── SimulationTimeRange ───────────────────────────────────────────────────

    @Test
    void ensureValidTimeRangeCanBeCreated() {
        final var tr = validTimeRange();
        assertEquals(START, tr.startDateTime());
        assertEquals(END, tr.endDateTime());
    }

    @Test
    void ensureEndBeforeStartIsRejected() {
        assertThrows(Exception.class,
                () -> new SimulationTimeRange(END, START),
                "endDateTime before startDateTime must be rejected");
    }

    @Test
    void ensureEqualStartAndEndIsRejected() {
        assertThrows(Exception.class,
                () -> new SimulationTimeRange(START, START),
                "endDateTime equal to startDateTime must be rejected");
    }

    @Test
    void ensureNullStartIsRejected() {
        assertThrows(Exception.class, () -> new SimulationTimeRange(null, END));
    }

    @Test
    void ensureNullEndIsRejected() {
        assertThrows(Exception.class, () -> new SimulationTimeRange(START, null));
    }

    // ── SafetyThreshold ───────────────────────────────────────────────────────

    @Test
    void ensureValidSafetyThresholdCanBeCreated() {
        final var t = new SafetyThreshold(50.0, "kt");
        assertEquals(50.0, t.value(), 0.001);
        assertEquals("kt", t.unit());
    }

    @Test
    void ensureZeroSafetyThresholdValueIsRejected() {
        assertThrows(Exception.class, () -> new SafetyThreshold(0.0, "kt"),
                "Zero threshold value must be rejected");
    }

    @Test
    void ensureNegativeSafetyThresholdValueIsRejected() {
        assertThrows(Exception.class, () -> new SafetyThreshold(-1.0, "kt"),
                "Negative threshold value must be rejected");
    }

    @Test
    void ensureBlankThresholdUnitIsRejected() {
        assertThrows(Exception.class, () -> new SafetyThreshold(50.0, ""),
                "Blank threshold unit must be rejected");
    }

    @Test
    void ensureNullThresholdUnitIsRejected() {
        assertThrows(Exception.class, () -> new SafetyThreshold(50.0, null));
    }

    // ── SimulationReport ──────────────────────────────────────────────────────

    @Test
    void ensureValidReportCanBeCreated() {
        final var r = new SimulationReport("/path/to/file.txt", "content");
        assertEquals("/path/to/file.txt", r.filePath());
        assertEquals("content", r.content());
    }

    @Test
    void ensureBlankFilePathIsRejected() {
        assertThrows(Exception.class, () -> new SimulationReport("", "content"),
                "Blank filePath must be rejected");
    }

    @Test
    void ensureNullFilePathIsRejected() {
        assertThrows(Exception.class, () -> new SimulationReport(null, "content"));
    }

    @Test
    void ensureNullContentIsRejected() {
        assertThrows(Exception.class, () -> new SimulationReport("/path/file.txt", null));
    }

    @Test
    void ensureReportFilePathIsTrimmed() {
        final var r = new SimulationReport("  /path/file.txt  ", "content");
        assertEquals("/path/file.txt", r.filePath(),
                "filePath should be trimmed of leading/trailing whitespace");
    }

    // ── ValidationResult enum ─────────────────────────────────────────────────

    @Test
    void ensureValidationResultEnumHasThreeValues() {
        assertEquals(3, ValidationResult.values().length,
                "ValidationResult must have PASSED, FAILED, PENDING");
    }
}
