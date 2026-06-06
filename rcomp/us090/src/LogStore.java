import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe, shared in-memory log store.
 *
 * Holds the last MAX_EVENTS events and tracks currently active sessions.
 * Also appends every new entry to a flat file for persistence across restarts.
 *
 * All public methods are synchronized — safe to call from both the UDP receiver
 * thread and the HTTP server threads concurrently.
 */
public class LogStore {

    private static final int  MAX_EVENTS = 200;
    private static final Path LOG_FILE   = Paths.get("access_log.csv");

    /** Recent events — newest first. */
    private final Deque<LogEntry> recentEvents = new ArrayDeque<>();

    /**
     * Active sessions: key = username + "@" + service + ":" + ip + ":" + port
     * Value = the LOGIN_OK entry that opened the session.
     */
    private final Map<String, LogEntry> activeSessions = new HashMap<>();

    public LogStore() {
        loadFromFile();
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    public synchronized void add(LogEntry entry) {
        recentEvents.addFirst(entry);
        while (recentEvents.size() > MAX_EVENTS) {
            recentEvents.removeLast();
        }

        String key = sessionKey(entry);
        if (entry.isLoginOk()) {
            activeSessions.put(key, entry);
        } else if (entry.isSessionEnd()) {
            activeSessions.remove(key);
        }

        appendToFile(entry);
    }

    // ── Read (used by US91 HTTP handlers) ─────────────────────────────────────

    public synchronized String getRecentEventsJson(int limit) {
        StringBuilder sb = new StringBuilder("[");
        int count = 0;
        for (LogEntry e : recentEvents) {
            if (count >= limit) break;
            if (count > 0) sb.append(",");
            sb.append(e.toJson());
            count++;
        }
        return sb.append("]").toString();
    }

    public synchronized String getActiveSessionsJson() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (LogEntry e : activeSessions.values()) {
            if (!first) sb.append(",");
            sb.append(e.toJson());
            first = false;
        }
        return sb.append("]").toString();
    }

    // ── File persistence ──────────────────────────────────────────────────────

    private void appendToFile(LogEntry entry) {
        try (BufferedWriter w = Files.newBufferedWriter(
                LOG_FILE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(entry.toCsvLine());
            w.newLine();
        } catch (IOException e) {
            System.err.println("[LogStore] Could not write to log file: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        if (!Files.exists(LOG_FILE)) return;
        try (BufferedReader r = Files.newBufferedReader(LOG_FILE)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    // toCsvLine format: eventType|timestamp|username|ip|port|service
                    // which is exactly what parse() expects
                    LogEntry e = LogEntry.parse(line);
                    recentEvents.addLast(e);
                    if (recentEvents.size() > MAX_EVENTS) recentEvents.removeFirst();
                } catch (Exception ex) {
                    System.err.println("[LogStore] Skipping malformed line: " + line);
                }
            }
            System.out.println("[LogStore] Loaded " + recentEvents.size()
                    + " entries from " + LOG_FILE);
        } catch (IOException e) {
            System.err.println("[LogStore] Could not read log file: " + e.getMessage());
        }
    }

    private static String sessionKey(LogEntry e) {
        return e.getUsername() + "@" + e.getService()
             + ":" + e.getClientIp() + ":" + e.getClientPort();
    }
}
