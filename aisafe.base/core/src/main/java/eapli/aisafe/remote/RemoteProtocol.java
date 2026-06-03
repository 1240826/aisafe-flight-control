package eapli.aisafe.remote;

/**
 * Shared protocol constants and helpers for all TCP remote services (US044, US078, US086).
 *
 * <p>Protocol rules:
 * <ul>
 *   <li>All messages are single lines terminated with {@code \n}, UTF-8.</li>
 *   <li>Fields are separated by {@value #SEP}.</li>
 *   <li>First field is always the command/status code.</li>
 * </ul>
 *
 * <p>Auth flow:
 * <pre>
 *   Client → AUTH|username|password
 *   Server → AUTH_OK   |   AUTH_FAIL|reason
 *   Client → COMMAND|arg1|arg2|...
 *   Server → OK|data   |   ERR|reason
 *   Client → QUIT
 *   Server → BYE
 * </pre>
 */
public final class RemoteProtocol {

    // ── Ports ───────────────────────────────────────────────────────────────
    public static final int WEATHER_PORT = 1044;
    public static final int ATC_PORT     = 1078;
    public static final int PILOT_PORT   = 1086;

    public static final int UDP_LOG_PORT  = 9090;
    public static final int HTTP_LOG_PORT = 8080;

    // ── Separator ───────────────────────────────────────────────────────────
    public static final String SEP = "|";

    // ── Client commands ─────────────────────────────────────────────────────
    public static final String CMD_AUTH       = "AUTH";
    public static final String CMD_QUIT       = "QUIT";

    // Weather (US044)
    public static final String CMD_REGISTER_WEATHER = "REGISTER_WEATHER";
    public static final String CMD_IMPORT_WEATHER   = "IMPORT_WEATHER";
    public static final String CMD_CONSULT_WEATHER  = "CONSULT_WEATHER";
    public static final String CMD_LIST_AREAS       = "LIST_AREAS";

    // ATC (US078)
    public static final String CMD_ADD_AIRCRAFT         = "ADD_AIRCRAFT";
    public static final String CMD_DECOMMISSION_AIRCRAFT = "DECOMMISSION_AIRCRAFT";
    public static final String CMD_LIST_FLEET           = "LIST_FLEET";
    public static final String CMD_CREATE_ROUTE         = "CREATE_ROUTE";
    public static final String CMD_DELETE_ROUTE         = "DELETE_ROUTE";
    public static final String CMD_ADD_PILOT            = "ADD_PILOT";
    public static final String CMD_LIST_PILOTS          = "LIST_PILOTS";
    public static final String CMD_REMOVE_PILOT         = "REMOVE_PILOT";

    // Pilot (US086)
    public static final String CMD_IMPORT_FLIGHT_PLAN = "IMPORT_FLIGHT_PLAN";
    public static final String CMD_LIST_ROUTES        = "LIST_ROUTES";
    public static final String CMD_LIST_FLIGHTS       = "LIST_FLIGHTS";

    // ── Server responses ─────────────────────────────────────────────────────
    public static final String RESP_AUTH_OK   = "AUTH_OK";
    public static final String RESP_AUTH_FAIL = "AUTH_FAIL";
    public static final String RESP_OK        = "OK";
    public static final String RESP_ERR       = "ERR";
    public static final String RESP_BYE       = "BYE";

    // ── UDP event types (US090) ───────────────────────────────────────────────
    public static final String EVENT_LOGIN_OK   = "LOGIN_OK";
    public static final String EVENT_LOGIN_FAIL = "LOGIN_FAIL";
    public static final String EVENT_LOGOUT     = "LOGOUT";
    public static final String EVENT_DISCONNECT = "DISCONNECT";

    // ── Service identifiers (US090) ──────────────────────────────────────────
    public static final String SVC_WEATHER = "US44";
    public static final String SVC_ATC     = "US78";
    public static final String SVC_PILOT   = "US86";

    private RemoteProtocol() { }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Build a success response line: {@code OK|data\n} or {@code OK\n} if data is null. */
    public static String ok(final String data) {
        return data == null || data.isEmpty()
                ? RESP_OK + "\n"
                : RESP_OK + SEP + data + "\n";
    }

    /** Build an error response line: {@code ERR|reason\n}. */
    public static String err(final String reason) {
        return RESP_ERR + SEP + (reason == null ? "Unknown error" : reason) + "\n";
    }

    /** Parse a received line into its fields (split on {@value #SEP}). */
    public static String[] parse(final String line) {
        if (line == null) return new String[0];
        return line.trim().split("\\|", -1);
    }

    /** Build a UDP log datagram string for US090. */
    public static String udpEvent(final String eventType, final String username,
                                   final String clientIp, final int clientPort,
                                   final String service) {
        final String ts = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return eventType + SEP + ts + SEP + username + SEP + clientIp + SEP + clientPort + SEP + service;
    }
}
