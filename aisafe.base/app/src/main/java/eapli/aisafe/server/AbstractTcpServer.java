package eapli.aisafe.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Generic TCP server that accepts connections and dispatches each one to a handler thread.
 *
 * <p>Subclasses only need to implement {@link #createHandler(Socket)} to return the
 * appropriate {@link AbstractClientHandler} for their service.
 */
public abstract class AbstractTcpServer implements Runnable {

    private static final int MAX_CLIENTS = 20;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final int port;
    private final String serverLabel;
    private final ExecutorService pool;
    private volatile boolean running;

    protected AbstractTcpServer(final int port, final String serverLabel) {
        this.port = port;
        this.serverLabel = serverLabel;
        this.pool = Executors.newFixedThreadPool(MAX_CLIENTS);
        this.running = true;
    }

    @Override
    public final void run() {
        System.out.printf("[%s] Starting TCP server on port %d...%n", serverLabel, port);
        try (final ServerSocket server = new ServerSocket(port)) {
            server.setSoTimeout(1000); // wake every 1s to check running flag
            while (running) {
                try {
                    final Socket client = server.accept();
                    System.out.printf("[%s] Client connected: %s:%d%n",
                            serverLabel,
                            client.getInetAddress().getHostAddress(),
                            client.getPort());
                    pool.submit(createHandler(client));
                } catch (final java.net.SocketTimeoutException e) {
                    // expected — just loop and re-check running
                } catch (final IOException e) {
                    if (running) {
                        System.err.printf("[%s] Accept error: %s%n", serverLabel, e.getMessage());
                    }
                }
            }
        } catch (final IOException e) {
            System.err.printf("[%s] Cannot bind to port %d: %s%n", serverLabel, port, e.getMessage());
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (final InterruptedException ie) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.printf("[%s] Server stopped.%n", serverLabel);
        }
    }

    /** Stop the server gracefully. */
    public void stop() {
        running = false;
    }

    /**
     * Factory method: return a {@link Runnable} handler for the given client socket.
     * Called once per accepted connection.
     */
    protected abstract Runnable createHandler(Socket clientSocket);
}
