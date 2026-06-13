package eapli.aisafe.remote;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * UDP client that sends remote access events to the Remote Accesses Logging Server (US090).
 *
 * <p>Each call is fire-and-forget: the datagram is sent and any I/O error is silently swallowed
 * so that a logging failure never interrupts the business operation.
 *
 * <p>Datagram format (single UTF-8 line):
 * {@code eventType|ISOdatetime|username|clientIP|clientPort|service}
 */
public final class UdpAccessLogger {

    private final String logHost;
    private final int logPort;

    public UdpAccessLogger(final String logHost, final int logPort) {
        this.logHost = logHost;
        this.logPort = logPort;
    }

    /** Convenience constructor using the default logging port from {@link RemoteProtocol}. */
    public UdpAccessLogger(final String logHost) {
        this(logHost, RemoteProtocol.UDP_LOG_PORT);
    }

    /** Send a LOGIN_OK event. */
    public void loginOk(final String username, final String clientIp,
                        final int clientPort, final String service) {
        send(RemoteProtocol.EVENT_LOGIN_OK, username, clientIp, clientPort, service);
    }

    /** Send a LOGIN_FAIL event. */
    public void loginFail(final String username, final String clientIp,
                          final int clientPort, final String service) {
        send(RemoteProtocol.EVENT_LOGIN_FAIL, username, clientIp, clientPort, service);
    }

    /** Send a LOGOUT event (client sent QUIT). */
    public void logout(final String username, final String clientIp,
                       final int clientPort, final String service) {
        send(RemoteProtocol.EVENT_LOGOUT, username, clientIp, clientPort, service);
    }

    /** Send a DISCONNECT event (client closed connection without QUIT). */
    public void disconnect(final String username, final String clientIp,
                           final int clientPort, final String service) {
        send(RemoteProtocol.EVENT_DISCONNECT, username, clientIp, clientPort, service);
    }

    private void send(final String eventType, final String username,
                      final String clientIp, final int clientPort, final String service) {
        final String payload = RemoteProtocol.udpEvent(eventType, username, clientIp, clientPort, service);
        try (final DatagramSocket socket = new DatagramSocket()) {
            final byte[] data = payload.getBytes(StandardCharsets.UTF_8);
            final InetAddress address = InetAddress.getByName(logHost);
            final DatagramPacket packet = new DatagramPacket(data, data.length, address, logPort);
            socket.send(packet);
        } catch (final Exception e) {
            // Fire-and-forget: logging must never interrupt business operations
            System.err.println("[UdpAccessLogger] Failed to send event: " + e.getMessage());
        }
    }
}
