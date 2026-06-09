package rcomp.logging;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * US91 — HTML page handler.
 *
 * Serves one of the two monitoring pages:
 *   /events -> last recorded access events
 *   /active -> currently active (logged-in) users
 *
 * Each page uses XMLHttpRequest (AJAX) to periodically poll the JSON API
 * and update the table without reloading the page, as required by US91.
 */
public class PageHandler implements HttpHandler {

    private final String page; // "events" | "active"

    public PageHandler(String page) {
        this.page = page;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] body = buildPage().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
        exchange.close();
    }

    private String buildPage() {
        boolean isEvents = "events".equals(page);
        String title     = isEvents ? "Last Recorded Events" : "Active Users";
        String apiUrl    = isEvents ? "/api/events"           : "/api/active";

        String tableHeaders = isEvents
                ? "<th>Timestamp</th><th>Service</th><th>Username</th>"
                  + "<th>Client IP</th><th>Port</th><th>Event</th>"
                : "<th>Login Time</th><th>Service</th><th>Username</th>"
                  + "<th>Client IP</th><th>Port</th>";

        String buildRowJs = isEvents
                ? "function buildRow(e) {\n"
                + "    var cls = '';\n"
                + "    if (e.eventType === 'LOGIN_OK')   cls = 'ok';\n"
                + "    if (e.eventType === 'LOGIN_FAIL')  cls = 'fail';\n"
                + "    if (e.eventType === 'LOGOUT')      cls = 'logout';\n"
                + "    if (e.eventType === 'DISCONNECT')  cls = 'disc';\n"
                + "    return '<tr>'\n"
                + "         + '<td>' + e.timestamp  + '</td>'\n"
                + "         + '<td>' + e.service    + '</td>'\n"
                + "         + '<td>' + e.username   + '</td>'\n"
                + "         + '<td>' + e.clientIp   + '</td>'\n"
                + "         + '<td>' + e.clientPort + '</td>'\n"
                + "         + '<td class=\"' + cls + '\">' + e.eventType + '</td>'\n"
                + "         + '</tr>';\n"
                + "}"
                : "function buildRow(e) {\n"
                + "    return '<tr>'\n"
                + "         + '<td>' + e.timestamp  + '</td>'\n"
                + "         + '<td>' + e.service    + '</td>'\n"
                + "         + '<td>' + e.username   + '</td>'\n"
                + "         + '<td>' + e.clientIp   + '</td>'\n"
                + "         + '<td>' + e.clientPort + '</td>'\n"
                + "         + '</tr>';\n"
                + "}";

        String ajaxJs =
                "function refreshTable() {\n"
                + "    var request = new XMLHttpRequest();\n"
                + "    request.onload = function() {\n"
                + "        var data = JSON.parse(this.responseText);\n"
                + "        var tbody = document.getElementById('tbody');\n"
                + "        var rows = '';\n"
                + "        for (var i = 0; i < data.length; i++) {\n"
                + "            rows += buildRow(data[i]);\n"
                + "        }\n"
                + "        tbody.innerHTML = rows;\n"
                + "        document.getElementById('status').innerHTML =\n"
                + "            'Last updated: ' + new Date().toLocaleTimeString()\n"
                + "            + ' &mdash; ' + data.length + ' record(s)';\n"
                + "        setTimeout(refreshTable, 3000);\n"
                + "    };\n"
                + "    request.ontimeout = function() {\n"
                + "        document.getElementById('status').innerHTML = 'Server timeout, retrying...';\n"
                + "        setTimeout(refreshTable, 1000);\n"
                + "    };\n"
                + "    request.onerror = function() {\n"
                + "        document.getElementById('status').innerHTML = 'No server reply, retrying...';\n"
                + "        setTimeout(refreshTable, 5000);\n"
                + "    };\n"
                + "    request.open('GET', '" + apiUrl + "', true);\n"
                + "    request.timeout = 5000;\n"
                + "    request.send();\n"
                + "}";

        return "<!DOCTYPE html>\n"
             + "<html>\n"
             + "<head>\n"
             + "  <meta charset=\"UTF-8\">\n"
             + "  <title>AISafe &mdash; " + title + "</title>\n"
             + "  <style>\n"
             + "    body  { font-family: Arial, sans-serif; background: #f4f4f4; margin: 20px; }\n"
             + "    h1    { color: #333; }\n"
             + "    nav a { margin-right: 16px; color: #0066cc; text-decoration: none; }\n"
             + "    nav a:hover { text-decoration: underline; }\n"
             + "    table { width: 100%; border-collapse: collapse; margin-top: 12px; background: white; }\n"
             + "    th    { background: #336699; color: white; padding: 8px 12px; text-align: left; }\n"
             + "    td    { padding: 7px 12px; border-bottom: 1px solid #ddd; }\n"
             + "    tr:hover td { background: #eef4ff; }\n"
             + "    .ok     { color: green;  font-weight: bold; }\n"
             + "    .fail   { color: red;    font-weight: bold; }\n"
             + "    .logout { color: orange; }\n"
             + "    .disc   { color: grey;   }\n"
             + "    #status { color: #666; font-size: 0.85em; margin-top: 6px; }\n"
             + "  </style>\n"
             + "</head>\n"
             + "<body onload=\"refreshTable()\">\n"
             + "  <h1>AISafe &mdash; Remote Access Logs</h1>\n"
             + "  <nav>\n"
             + "    <a href=\"/events\">Last Events</a>\n"
             + "    <a href=\"/active\">Active Users</a>\n"
             + "  </nav>\n"
             + "  <h2>" + title + "</h2>\n"
             + "  <div id=\"status\">Loading...</div>\n"
             + "  <table>\n"
             + "    <thead><tr>" + tableHeaders + "</tr></thead>\n"
             + "    <tbody id=\"tbody\"></tbody>\n"
             + "  </table>\n"
             + "  <script>\n"
             + buildRowJs + "\n"
             + ajaxJs + "\n"
             + "  </script>\n"
             + "</body>\n"
             + "</html>\n";
    }
}
