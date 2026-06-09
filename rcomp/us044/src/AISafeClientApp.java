package rcomp.client;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * US044 — Weather Person remote client application.
 *
 * <p>Connects to the AISafe Main Application via TCP and exposes all
 * Weather Person operations (US041, US042, US043) through a console menu.
 *
 * <p>Usage:
 * <pre>
 *   javac -d out rcomp/us044/src/*.java
 *   java  -cp out rcomp.client.AISafeClientApp [host] [port]
 * </pre>
 * Default: host=localhost, port=1044
 */
public final class AISafeClientApp {

    private static final int DEFAULT_PORT = 1044;

    private AISafeClientApp() { }

    public static void main(final String[] args) {
        final String host = args.length > 0 ? args[0] : "localhost";
        final int    port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        System.out.println("==========================================");
        System.out.println("  AISafe — Weather Person Client (US044) ");
        System.out.println("==========================================");
        System.out.printf("Connecting to %s:%d ...%n", host, port);

        try (final TcpClient client = new TcpClient(host, port);
             final Scanner sc = new Scanner(System.in)) {

            System.out.println("Connected.");

            if (!login(client, sc)) {
                System.out.println("Authentication failed. Exiting.");
                return;
            }
            System.out.println("Authenticated.");

            mainLoop(client, sc);

        } catch (final IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    private static boolean login(final TcpClient c, final Scanner sc) throws IOException {
        System.out.print("Username: "); final String user = sc.nextLine().trim();
        System.out.print("Password: "); final String pass = sc.nextLine().trim();
        final String resp = c.send("AUTH|" + user + "|" + pass);
        System.out.println("Server: " + resp);
        return resp != null && resp.startsWith("AUTH_OK");
    }

    // ── Menu ─────────────────────────────────────────────────────────────────

    private static void mainLoop(final TcpClient c, final Scanner sc) throws IOException {
        while (true) {
            System.out.println();
            System.out.println("──── Weather Menu ────────────────────────");
            System.out.println(" 1. Register weather data");
            System.out.println(" 2. Import bulk weather data (CSV)");
            System.out.println(" 3. Consult weather data");
            System.out.println(" 4. List air control areas");
            System.out.println(" 0. Logout & Exit");
            System.out.print("Option: ");

            switch (sc.nextLine().trim()) {
                case "1" -> doRegisterWeather(c, sc);
                case "2" -> doImportWeather(c, sc);
                case "3" -> doConsultWeather(c, sc);
                case "4" -> printResp(c.send("LIST_AREAS"));
                case "0" -> { System.out.println("Goodbye."); return; }
                default  -> System.out.println("Invalid option.");
            }
        }
    }

    // ── Operations ────────────────────────────────────────────────────────────

    private static void doRegisterWeather(final TcpClient c, final Scanner sc) throws IOException {
        System.out.print("Area code: ");          final String area  = sc.nextLine().trim();
        if (area.isEmpty()) { System.out.println("Area code required."); return; }
        System.out.print("Latitude: ");           final String lat   = sc.nextLine().trim();
        if (!isDouble(lat, "Latitude")) return;
        System.out.print("Longitude: ");          final String lon   = sc.nextLine().trim();
        if (!isDouble(lon, "Longitude")) return;
        System.out.print("Altitude (m): ");       final String alt   = sc.nextLine().trim();
        if (!isDouble(alt, "Altitude")) return;
        System.out.print("Wind speed (knots): "); final String speed = sc.nextLine().trim();
        if (!isDouble(speed, "Wind speed")) return;
        System.out.print("Wind dir (deg): ");     final String dir   = sc.nextLine().trim();
        if (!isDouble(dir, "Wind direction")) return;
        System.out.print("Temperature (°C): ");   final String temp  = sc.nextLine().trim();
        if (!isDouble(temp, "Temperature")) return;
        System.out.print("Provider: ");           final String prov  = sc.nextLine().trim();
        System.out.print("DateTime (e.g. 2026-06-01T14:30:00): "); final String dt = sc.nextLine().trim();
        if (!isDateTime(dt)) return;

        printResp(c.send("REGISTER_WEATHER|" + area + "|" + lat + "|" + lon + "|"
                + alt + "|" + speed + "|" + dir + "|" + temp + "|" + prov + "|" + dt));
    }

    private static void doImportWeather(final TcpClient c, final Scanner sc) throws IOException {
        System.out.print("Area code: ");
        final String area = sc.nextLine().trim();
        if (area.isEmpty()) { System.out.println("Area code required."); return; }
        System.out.println("CSV rows (lat,lon,alt,speed,dir,temp,provider[,datetime])");
        System.out.println("Separate with  ;  — blank line to finish:");
        final StringBuilder csv = new StringBuilder();
        String line;
        while (!(line = sc.nextLine()).isBlank()) {
            if (csv.length() > 0) csv.append(";");
            csv.append(line.trim());
        }
        if (csv.isEmpty()) { System.out.println("No data entered."); return; }
        printResp(c.send("IMPORT_WEATHER|" + area + "|" + csv));
    }

    private static void doConsultWeather(final TcpClient c, final Scanner sc) throws IOException {
        System.out.print("Area code: ");
        final String area = sc.nextLine().trim();
        if (area.isEmpty()) { System.out.println("Area code required."); return; }
        System.out.print("Date (e.g. 2026-06-01): ");
        final String date = sc.nextLine().trim();
        if (!isDate(date)) return;
        printResp(c.send("CONSULT_WEATHER|" + area + "|" + date));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static boolean isDouble(final String s, final String field) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (final NumberFormatException e) {
            System.out.println("Invalid " + field + ": must be a number.");
            return false;
        }
    }

    private static boolean isDate(final String s) {
        try {
            LocalDate.parse(s);
            return true;
        } catch (final DateTimeParseException e) {
            System.out.println("Invalid date. Use yyyy-MM-dd (e.g. 2026-06-01).");
            return false;
        }
    }

    private static boolean isDateTime(final String s) {
        try {
            LocalDateTime.parse(s);
            return true;
        } catch (final DateTimeParseException e) {
            System.out.println("Invalid datetime. Use ISO format (e.g. 2026-06-01T14:30:00).");
            return false;
        }
    }

    private static void printResp(final String resp) {
        if (resp == null)                 { System.out.println("Connection closed by server."); return; }
        if (resp.startsWith("OK|"))       { System.out.println("✔  " + resp.substring(3)); }
        else if (resp.startsWith("ERR|")) { System.out.println("  Error: " + resp.substring(4)); }
        else                              { System.out.println("   " + resp); }
    }
}
