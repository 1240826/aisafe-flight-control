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
        if (jsonInput == null) {
            throw new SimulationRunnerException("JSON input must not be null");
        }

        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(timeoutSeconds * 1000);

            final byte[] jsonBytes = jsonInput.getBytes(StandardCharsets.UTF_8);
            final var out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(jsonBytes.length);
            out.write(jsonBytes);
            out.flush();

            final var in = new DataInputStream(socket.getInputStream());
            final int reportLen = in.readInt();
            if (reportLen <= 0 || reportLen > 10 * 1024 * 1024) {
                throw new SimulationRunnerException(
                        "Invalid report length from simulator: " + reportLen);
            }

            final byte[] reportBytes = new byte[reportLen];
            in.readFully(reportBytes);
            return new String(reportBytes, StandardCharsets.UTF_8);

        } catch (final java.net.SocketTimeoutException e) {
            throw new SimulationRunnerException(
                    "Simulator timed out after " + timeoutSeconds + " seconds", e);
        } catch (final IOException e) {
            throw new SimulationRunnerException(
                    "I/O error connecting to simulator at " + host + ":" + port, e);
        }
    }
}
