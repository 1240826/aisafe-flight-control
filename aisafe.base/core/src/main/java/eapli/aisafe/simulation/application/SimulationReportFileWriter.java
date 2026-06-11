package eapli.aisafe.simulation.application;

import eapli.aisafe.simulation.domain.Simulation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimulationReportFileWriter {

    public String writeToFile(final Simulation simulation, final String outputPath) {
        final String content = simulation.report().content();
        try {
            final Path path = Path.of(outputPath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            return outputPath;
        } catch (final IOException e) {
            throw new RuntimeException("Failed to write simulation report to " + outputPath, e);
        }
    }
}
