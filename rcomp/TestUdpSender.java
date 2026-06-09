import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Quick test — sends fake UDP log datagrams to the US90 server.
 * Usage: java TestUdpSender [host] [port]
 */
public class TestUdpSender {

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int    port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;

        String[] events = {
            "LOGIN_OK|2026-06-05T14:30:00|alice|127.0.0.1|54321|US44",
            "LOGIN_OK|2026-06-05T14:30:05|bob|127.0.0.1|54322|US78",
            "LOGIN_FAIL|2026-06-05T14:30:10|hacker|127.0.0.1|54323|US44",
            "LOGIN_OK|2026-06-05T14:30:15|carol|127.0.0.1|54324|US86",
            "LOGOUT|2026-06-05T14:30:20|alice|127.0.0.1|54321|US44"
        };

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(host);
            for (String msg : events) {
                byte[] data = msg.getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(data, data.length, address, port));
                System.out.println("Sent: " + msg);
                Thread.sleep(500);
            }
        }
        System.out.println("Done. Check http://" + host + ":8080/events");
    }
}
