package rcomp.logging;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Represents one remote-access event logged by the system (US90).
 *
 * Datagram format sent by UdpAccessLogger (eapli.aisafe.remote):
 *   eventType|timestamp|username|clientIP|clientPort|service
 *
 * Example:
 *   LOGIN_OK|2026-06-05T14:30:00|alice|192.168.1.10|54321|US44
 *
 * eventType : LOGIN_OK | LOGIN_FAIL | LOGOUT | DISCONNECT
 * service   : US44 | US78 | US86
 */
public class LogEntry {

    private static final DateTimeFormatter PARSE_FMT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Instant timestamp;
    private final String  username;
    private final String  clientIp;
    private final int     clientPort;
    private final String  service;
    private final String  eventType;

    public LogEntry(Instant timestamp, String username, String clientIp,
                    int clientPort, String service, String eventType) {
        this.timestamp  = timestamp;
        this.username   = username;
        this.clientIp   = clientIp;
        this.clientPort = clientPort;
        this.service    = service;
        this.eventType  = eventType;
    }

    /**
     * Parse a UDP datagram sent by UdpAccessLogger.
     *
     * Expected format:
     *   eventType|timestamp|username|clientIP|clientPort|service
     *
     * Example:
     *   LOGIN_OK|2026-06-05T14:30:00|alice|192.168.1.10|54321|US44
     */
    public static LogEntry parse(String raw) throws Exception {
        String[] parts = raw.trim().split("\\|", -1);
        if (parts.length < 6) {
            throw new Exception("Invalid datagram (expected 6 fields): " + raw);
        }
        String  evt  = parts[0].trim();
        Instant ts   = LocalDateTime.parse(parts[1].trim(), PARSE_FMT)
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant();
        String  user = parts[2].trim();
        String  ip   = parts[3].trim();
        int     port = Integer.parseInt(parts[4].trim());
        String  svc  = parts[5].trim();
        return new LogEntry(ts, user, ip, port, svc, evt);
    }

    public boolean isLoginOk()    { return "LOGIN_OK".equals(eventType); }
    public boolean isSessionEnd() { return "LOGOUT".equals(eventType) || "DISCONNECT".equals(eventType); }

    public Instant getTimestamp()  { return timestamp; }
    public String  getUsername()   { return username; }
    public String  getClientIp()   { return clientIp; }
    public int     getClientPort() { return clientPort; }
    public String  getService()    { return service; }
    public String  getEventType()  { return eventType; }

    public String toJson() {
        String displayTs = DISPLAY_FMT.format(timestamp.atZone(ZoneId.systemDefault()));
        return "{\"timestamp\":\"" + displayTs          + "\","
             + "\"username\":\""   + escapeJson(username)  + "\","
             + "\"clientIp\":\""   + escapeJson(clientIp)  + "\","
             + "\"clientPort\":"   + clientPort             + ","
             + "\"service\":\""    + escapeJson(service)   + "\","
             + "\"eventType\":\""  + escapeJson(eventType) + "\"}";
    }

    public String toCsvLine() {
        return eventType + "|"
             + PARSE_FMT.format(timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime())
             + "|" + username + "|" + clientIp + "|" + clientPort + "|" + service;
    }

    @Override
    public String toString() {
        return DISPLAY_FMT.format(timestamp.atZone(ZoneId.systemDefault()))
             + " [" + service + "] " + eventType
             + " user=" + username + " " + clientIp + ":" + clientPort;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
