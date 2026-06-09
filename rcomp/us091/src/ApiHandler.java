package rcomp.logging;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * US91 — JSON API handler.
 *
 * GET /api/events -> JSON array of last recorded events
 * GET /api/active -> JSON array of currently active sessions
 *
 * Called periodically by XMLHttpRequest (AJAX) from the browser pages.
 */
public class ApiHandler implements HttpHandler {

    private static final int DEFAULT_LIMIT = 50;

    private final LogStore store;
    private final boolean  activeOnly;

    public ApiHandler(LogStore store, boolean activeOnly) {
        this.store      = store;
        this.activeOnly = activeOnly;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        String json = activeOnly
                ? store.getActiveSessionsJson()
                : store.getRecentEventsJson(DEFAULT_LIMIT);

        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
        exchange.close();
    }
}
