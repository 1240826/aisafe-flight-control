package eapli.aisafe.simulation.application;

import eapli.aisafe.simulation.domain.Simulation;
import eapli.aisafe.simulation.domain.SimulationReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimulationReportFileWriterTest {

    @TempDir
    Path tempDir;

    private final SimulationReportFileWriter writer = new SimulationReportFileWriter();

    @Test
    void ensureWriteToFileCreatesFileWithCorrectContent() throws IOException {
        // Arrange
        final String content = "SCOMP simulation output line 1\nline 2";
        final String outputPath = tempDir.resolve("report.txt").toString();
        final SimulationReport report = new SimulationReport("/original/path.txt", content);
        final Simulation simulation = mock(Simulation.class);
        when(simulation.report()).thenReturn(report);

        // Act
        writer.writeToFile(simulation, outputPath);

        // Assert
        assertEquals(content, Files.readString(Path.of(outputPath)));
    }

    @Test
    void ensureWriteToFileReturnsOutputPath() {
        // Arrange
        final String outputPath = tempDir.resolve("out.txt").toString();
        final SimulationReport report = new SimulationReport("/path.txt", "content");
        final Simulation simulation = mock(Simulation.class);
        when(simulation.report()).thenReturn(report);

        // Act
        final String result = writer.writeToFile(simulation, outputPath);

        // Assert
        assertEquals(outputPath, result);
    }

    @Test
    void ensureWriteToFileThrowsRuntimeExceptionOnInvalidPath() {
        // Arrange — path points to a non-existent parent directory
        final String badPath = tempDir.resolve("nonexistent-dir/report.txt").toString();
        final SimulationReport report = new SimulationReport("/path.txt", "content");
        final Simulation simulation = mock(Simulation.class);
        when(simulation.report()).thenReturn(report);

        // Act / Assert
        assertThrows(RuntimeException.class,
                () -> writer.writeToFile(simulation, badPath),
                "Write to an invalid path must throw RuntimeException");
    }
}
