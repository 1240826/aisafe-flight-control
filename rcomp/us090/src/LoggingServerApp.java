package rcomp.logging;

import java.io.IOException;

/**
 * Remote Accesses Logging Server — main entry point.
 *
 * Starts two services in the same process:
 *   US90 — UDP receiver thread : receives log datagrams from the Main Application
 *   US91 — HTTP server         : serves the two AJAX monitoring pages
 *
 * Both share the same LogStore instance.
 *
 * Usage:
 *   java -cp out rcomp.logging.LoggingServerApp <udpPort> <httpPort>
 *
 * Example:
 *   java -cp out rcomp.logging.LoggingServerApp 9090 8080
 *
 * Defaults:
 *   udpPort  = 9090
 *   httpPort = 8080
 */
public class LoggingServerApp {

    private static final int DEFAULT_UDP_PORT  = 9090;
    private static final int DEFAULT_HTTP_PORT = 8080;

    public static void main(String[] args) {
        int udpPort  = DEFAULT_UDP_PORT;
        int httpPort = DEFAULT_HTTP_PORT;

        if (args.length >= 1) {
            try { udpPort  = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) {
                System.err.println("Invalid UDP port: " + args[0]); System.exit(1);
            }
        }
        if (args.length >= 2) {
            try { httpPort = Integer.parseInt(args[1]); }
            catch (NumberFormatException e) {
                System.err.println("Invalid HTTP port: " + args[1]); System.exit(1);
            }
        }

        System.out.println("==========================================");
        System.out.println("  AISafe - Remote Accesses Logging Server");
        System.out.println("  US90 (UDP receiver) + US91 (HTTP pages)");
        System.out.println("==========================================");
        System.out.println("  UDP  port : " + udpPort  + "  <- datagrams from Main App");
        System.out.println("  HTTP port : " + httpPort + "  <- browser monitoring pages");
        System.out.println("==========================================");

        // Shared in-memory store (thread-safe, persists to access_log.csv)
        LogStore store = new LogStore();

        // US91 — HTTP server
        HttpLogServer httpServer = new HttpLogServer(httpPort, store);
        try {
            httpServer.start();
        } catch (IOException e) {
            System.err.println("[Main] Failed to start HTTP server: " + e.getMessage());
            System.exit(1);
        }

        // US90 — UDP receiver thread
        Thread udpThread = new Thread(new UdpLogReceiver(udpPort, store), "udp-log-receiver");
        udpThread.setDaemon(false);
        udpThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Main] Shutting down...");
            udpThread.interrupt();
        }));

        System.out.println("[Main] Logging server running. Press Ctrl+C to stop.");
    }
}
