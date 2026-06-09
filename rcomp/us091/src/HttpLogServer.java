package rcomp.logging;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * US91 — HTTP server for the Remote Accesses Logging Server.
 *
 * Endpoints:
 *   GET /         -> redirect to /events
 *   GET /events   -> HTML page: last recorded events (AJAX auto-refresh)
 *   GET /active   -> HTML page: currently active users (AJAX auto-refresh)
 *   GET /api/events -> JSON consumed by /events page
 *   GET /api/active -> JSON consumed by /active page
 */
public class HttpLogServer {

    private final int      httpPort;
    private final LogStore store;

    public HttpLogServer(int httpPort, LogStore store) {
        this.httpPort = httpPort;
        this.store    = store;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(httpPort), 0);

        server.createContext("/events",     new PageHandler("events"));
        server.createContext("/active",     new PageHandler("active"));
        server.createContext("/api/events", new ApiHandler(store, false));
        server.createContext("/api/active", new ApiHandler(store, true));
        server.createContext("/",           new RedirectHandler("/events"));

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("[US91] HTTP server listening on port " + httpPort);
        System.out.println("[US91]   http://<host>:" + httpPort + "/events");
        System.out.println("[US91]   http://<host>:" + httpPort + "/active");
    }

    private static class RedirectHandler implements HttpHandler {
        private final String target;
        RedirectHandler(String target) { this.target = target; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"/".equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            exchange.getResponseHeaders().set("Location", target);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        }
    }
}
