package eapli.aisafe.simulation.application;

import eapli.aisafe.simulation.domain.Simulation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes a simulation report to a file and returns the output path.
 */
public class SimulationReportFileWriter {

    /**
     * Writes the simulation report content to the specified output path.
     *
     * @param simulation the simulation whose report content to write (non-null)
     * @param outputPath destination path for the report file (non-blank)
     * @return the outputPath after writing
     * @throws RuntimeException wrapping IOException if writing fails
     */
    public String writeToFile(final Simulation simulation, final String outputPath) {
        final String content = simulation.report().content();
        try {
            Files.writeString(Path.of(outputPath), content);
            return outputPath;
        } catch (final IOException e) {
            throw new RuntimeException("Failed to write simulation report to " + outputPath, e);
        }
    }
}
