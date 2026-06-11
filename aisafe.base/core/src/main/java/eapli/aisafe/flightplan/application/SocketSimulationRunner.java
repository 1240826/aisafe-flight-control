package eapli.aisafe.flightplan.application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketSimulationRunner implements SimulationRunner {

    private final String host;
    private final int port;
    private final int timeoutSeconds;

    public SocketSimulationRunner(final String host, final int port, final int timeoutSeconds) {
        this.host = host;
        this.port = port;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String run(final String jsonInput) throws SimulationRunnerException {
        return run(jsonInput, null);
    }

    @Override
    public String run(final String jsonInput, final String weatherFilePath) throws SimulationRunnerException {
        if (jsonInput == null) {
            throw new SimulationRunnerException("JSON input must not be null");
        }

        System.err.println("[SIM] Connecting to " + host + ":" + port + "...");
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(timeoutSeconds * 1000);
            System.err.println("[SIM] Connected. Sending scenario (" + jsonInput.length() + " bytes)...");

            final var out = new DataOutputStream(socket.getOutputStream());

            // Send scenario JSON (length-prefixed)
            final byte[] jsonBytes = jsonInput.getBytes(StandardCharsets.UTF_8);
            out.writeInt(jsonBytes.length);
            out.write(jsonBytes);
            System.err.println("[SIM] Scenario sent (" + jsonBytes.length + " bytes).");

            // Send weather JSON if provided (length-prefixed, 0 = no weather)
            if (weatherFilePath != null && !weatherFilePath.isBlank()) {
                final byte[] weatherBytes = java.nio.file.Files.readAllBytes(
                        java.nio.file.Path.of(weatherFilePath));
                out.writeInt(weatherBytes.length);
                out.write(weatherBytes);
                System.err.println("[SIM] Weather sent (" + weatherBytes.length + " bytes).");
            } else {
                out.writeInt(0);
                System.err.println("[SIM] No weather data.");
            }
            out.flush();

            System.err.println("[SIM] Waiting for report...");
            final var in = new DataInputStream(socket.getInputStream());
            final int reportLen = in.readInt();
            System.err.println("[SIM] Report length: " + reportLen);
            if (reportLen <= 0 || reportLen > 10 * 1024 * 1024) {
                throw new SimulationRunnerException(
                        "Invalid report length from simulator: " + reportLen);
            }

            final byte[] reportBytes = new byte[reportLen];
            in.readFully(reportBytes);
            final String report = new String(reportBytes, StandardCharsets.UTF_8);
            System.err.println("[SIM] Report received (" + report.length() + " chars).");
            return report;

        } catch (final java.net.SocketTimeoutException e) {
            System.err.println("[SIM] TIMEOUT after " + timeoutSeconds + "s");
            throw new SimulationRunnerException(
                    "Simulator timed out after " + timeoutSeconds + " seconds", e);
        } catch (final IOException e) {
            System.err.println("[SIM] I/O ERROR: " + e.getMessage());
            throw new SimulationRunnerException(
                    "I/O error connecting to simulator at " + host + ":" + port, e);
        }
    }
}
