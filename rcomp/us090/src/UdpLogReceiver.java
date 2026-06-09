package rcomp.logging;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

/**
 * US90 — UDP log receiver.
 *
 * Listens on the configured UDP port for log datagrams sent by the Main
 * Application TCP servers (US44, US78, US86) and stores them in the
 * shared LogStore.
 *
 * Runs as a dedicated thread inside the Remote Accesses Logging Server.
 *
 * Datagram wire format (UTF-8, pipe-separated):
 *   EVENT|<epochMs>|<username>|<clientIP>|<clientPort>|<service>|<eventType>
 */
public class UdpLogReceiver implements Runnable {

    private static final int BUFFER_SIZE = 1024;

    private final int      udpPort;
    private final LogStore store;

    public UdpLogReceiver(int udpPort, LogStore store) {
        this.udpPort = udpPort;
        this.store   = store;
    }

    @Override
    public void run() {
        System.out.println("[US90] UDP receiver listening on port " + udpPort);
        try (DatagramSocket socket = new DatagramSocket(udpPort)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                handlePacket(packet);
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.err.println("[US90] UDP receiver error: " + e.getMessage());
            }
        }
        System.out.println("[US90] UDP receiver stopped.");
    }

    private void handlePacket(DatagramPacket packet) {
        String raw = new String(packet.getData(), 0, packet.getLength(),
                                StandardCharsets.UTF_8).trim();
        System.out.println("[US90] Datagram from "
                + packet.getAddress().getHostAddress() + ":" + packet.getPort()
                + " -> " + raw);
        try {
            LogEntry entry = LogEntry.parse(raw);
            store.add(entry);
            System.out.println("[US90] Stored: " + entry);
        } catch (Exception e) {
            System.err.println("[US90] Malformed datagram, ignoring: " + e.getMessage());
        }
    }
}
