package eapli.aisafe.simulation.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationReportTest {

    @Test
    void ensureValidSimulationReportCanBeCreated() {
        final var report = new SimulationReport("/tmp/sim_output.txt", "Simulation PASSED");
        assertEquals("/tmp/sim_output.txt", report.filePath());
        assertEquals("Simulation PASSED", report.content());
    }

    @Test
    void ensureFilePathMustNotBeBlank() {
        assertThrows(Exception.class,
                () -> new SimulationReport("", "content"));
        assertThrows(Exception.class,
                () -> new SimulationReport("   ", "content"));
    }

    @Test
    void ensureFilePathMustNotBeNull() {
        assertThrows(Exception.class,
                () -> new SimulationReport(null, "content"));
    }

    @Test
    void ensureContentMustNotBeNull() {
        assertThrows(Exception.class,
                () -> new SimulationReport("/tmp/file.txt", null));
    }

    @Test
    void ensureEqualsIgnoresContent() {
        final var r1 = new SimulationReport("/tmp/file.txt", "PASSED");
        final var r2 = new SimulationReport("/tmp/file.txt", "FAILED");
        assertEquals(r1, r2, "Equality is based on filePath only");
    }

    @Test
    void ensureNotEqualsDifferentFilePath() {
        final var r1 = new SimulationReport("/tmp/file1.txt", "content");
        final var r2 = new SimulationReport("/tmp/file2.txt", "content");
        assertNotEquals(r1, r2);
    }

    @Test
    void ensureHashCodeDependsOnFilePath() {
        final var r1 = new SimulationReport("/tmp/file.txt", "PASSED");
        final var r2 = new SimulationReport("/tmp/file.txt", "FAILED");
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void ensureToStringContainsFilePath() {
        final var r = new SimulationReport("/tmp/result.txt", "PASSED");
        assertTrue(r.toString().contains("/tmp/result.txt"));
    }

    @Test
    void ensureFilePathIsTrimmed() {
        final var r = new SimulationReport("  /tmp/result.txt  ", "PASSED");
        assertEquals("/tmp/result.txt", r.filePath());
    }

    @Test
    void ensureEmptyContentIsAllowed() {
        assertDoesNotThrow(() -> new SimulationReport("/tmp/file.txt", ""));
    }
}
