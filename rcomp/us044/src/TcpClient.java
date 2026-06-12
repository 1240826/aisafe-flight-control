package rcomp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Reusable TCP client for AISafe remote services.
 * Manages a single persistent connection and provides line-based send/receive.
 */
public final class TcpClient implements AutoCloseable {

    private static final int CONNECT_TIMEOUT_MS = 5_000;

    private final Socket       socket;
    private final BufferedReader in;
    private final PrintWriter    out;
    private String selectedArea;

    public TcpClient(final String host, final int port) throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        this.socket.setSoTimeout(30_000);
        this.in  = new BufferedReader(
                new InputStreamReader(socket.getInputStream(),  StandardCharsets.UTF_8));
        this.out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), false);
        this.selectedArea = null;
    }

    /**
     * Send a command line and return the server's single-line response.
     *
     * @param command the command string (without trailing newline)
     * @return the server response line, or {@code null} if the connection was closed
     */
    public String send(final String command) throws IOException {
        out.print(command + "\n");
        out.flush();
        return in.readLine();
    }

    /**
     * Get the currently selected area code.
     * @return the selected area code, or null if none is selected
     */
    public String getSelectedArea() {
        return selectedArea;
    }

    /**
     * Set the currently selected area code.
     * @param area the area code to set as selected
     */
    public void setSelectedArea(final String area) {
        this.selectedArea = area;
    }

    /**
     * Send a command with the selected area code if available.
     * @param baseCommand the base command without area parameter
     * @return the server response line, or {@code null} if no area selected
     */
    public String sendWithArea(final String baseCommand) throws IOException {
        if (selectedArea != null && !selectedArea.isEmpty()) {
            return send(baseCommand + "|" + selectedArea);
        }
        return null;
    }

    @Override
    public void close() {
        try { out.print("QUIT\n"); out.flush(); } catch (final Exception ignored) { }
        try { in.close();  } catch (final Exception ignored) { }
        out.close();
        try { socket.close(); } catch (final Exception ignored) { }
    }
}
