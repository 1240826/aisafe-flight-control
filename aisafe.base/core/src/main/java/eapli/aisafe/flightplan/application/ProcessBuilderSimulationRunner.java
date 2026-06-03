package eapli.aisafe.flightplan.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public class ProcessBuilderSimulationRunner implements SimulationRunner {

    private final String simulatorExecutable;
    private final int timeoutSeconds;

    public ProcessBuilderSimulationRunner(final String simulatorExecutable,
                                          final int timeoutSeconds) {
        this.simulatorExecutable = simulatorExecutable;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String run(final String jsonInput) throws SimulationRunnerException {
        if (jsonInput == null) {
            throw new SimulationRunnerException("JSON input must not be null");
        }
        try {
            final var tempInput = Files.createTempFile("sim_input_", ".json");
            Files.writeString(tempInput, jsonInput, StandardCharsets.UTF_8);

            final var tempOutput = Files.createTempFile("sim_output_", ".txt");
            final var processBuilder = new java.lang.ProcessBuilder(
                    simulatorExecutable, tempInput.toString(), tempOutput.toString());
            processBuilder.redirectErrorStream(true);

            final var process = processBuilder.start();

            final var reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            final var output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            final boolean exited = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!exited) {
                process.destroyForcibly();
                throw new SimulationRunnerException(
                        "Simulator timed out after " + timeoutSeconds + " seconds");
            }
            if (process.exitValue() != 0) {
                throw new SimulationRunnerException(
                        "Simulator process failed with exit code: " + process.exitValue()
                                + "\nOutput: " + output);
            }

            final var reportContent = Files.readString(tempOutput, StandardCharsets.UTF_8);

            Files.deleteIfExists(tempInput);
            Files.deleteIfExists(tempOutput);

            return reportContent;

        } catch (final IOException e) {
            throw new SimulationRunnerException("I/O error running simulator: " + e.getMessage(), e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SimulationRunnerException("Simulator process interrupted", e);
        }
    }
}
