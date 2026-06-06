package rcomp.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * US086 — Pilot (Flight Control Operator) remote client application.
 *
 * <p>Connects to the AISafe Main Application via TCP and exposes all
 * FCO operations (US072, US080, US085, US111, US112, US121) through
 * a console menu.
 *
 * <p>Usage:
 * <pre>
 *   javac -d out -sourcepath src src/rcomp/client/PilotClientApp.java
 *   java  -cp out rcomp.client.PilotClientApp [host] [port]
 * </pre>
 * Default: host=localhost, port=1086
 */
public final class PilotClientApp {

    private static final int DEFAULT_PORT = 1086;

    private PilotClientApp() { }

    public static void main(final String[] args) {
        final String host = args.length > 0 ? args[0] : "localhost";
        final int    port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        System.out.println("==========================================");
        System.out.println("  AISafe — Pilot Client (US086)");
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
            System.out.println("──── Pilot Menu ────────────────────────────");
            System.out.println(" 1. List company fleet (US072)");
            System.out.println(" 2. Create flight plan (US080)");
            System.out.println(" 3. Import flight plan from DSL (US121)");
            System.out.println(" 4. Validate flight plan (US085)");
            System.out.println(" 5. Generate simulation report (US111)");
            System.out.println(" 6. Monthly report (US112)");
            System.out.println(" 7. List flights");
            System.out.println(" 8. List routes");
            System.out.println(" 0. Logout & Exit");
            System.out.print("Option: ");

            switch (sc.nextLine().trim()) {
                case "1" -> printResp(c.send("LIST_FLEET"));
                case "2" -> doCreateFlightPlan(c, sc);
                case "3" -> doImportFlightPlan(c, sc);
                case "4" -> doValidateFlightPlan(c, sc);
                case "5" -> doGenerateReport(c, sc);
                case "6" -> doMonthlyReport(c, sc);
                case "7" -> printResp(c.send("LIST_FLIGHTS"));
                case "8" -> printResp(c.send("LIST_ROUTES"));
                case "0" -> { System.out.println("Goodbye."); return; }
                default  -> System.out.println("Invalid option.");
            }
        }
    }

    // ── Operations ────────────────────────────────────────────────────────────

    private static void doCreateFlightPlan(final TcpClient c, final Scanner sc) throws IOException {
        System.out.print("Flight ID: ");
        final String flightId = sc.nextLine().trim();
        System.out.println("DSL content (blank line to finish):");
        final String dsl = readMultiline(sc);
        if (dsl.isEmpty()) {
            System.out.println("Canceled.");
            return;
        }
        printResp(c.send("CREATE_FLIGHT_PLAN|" + flightId + "|" + dsl));
    }

    private static void doImportFlightPlan(final TcpClient c, final Scanner sc) throws IOException {
        System.out.print("Flight ID: ");
        final String flightId = sc.nextLine().trim();
        System.out.print("DSL file path: ");
        final String path = sc.nextLine().trim();
        try {
            final String dsl = Files.readString(Paths.get(path));
            printResp(c.send("IMPORT_FLIGHT_PLAN|" + flightId + "|" + dsl));
        } catch (final IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    private static void doValidateFlightPlan(final TcpClient c, final Scanner sc) throws IOException {
        System.out.print("Flight Plan ID: ");
        printResp(c.send("VALIDATE_FLIGHT_PLAN|" + sc.nextLine().trim()));
    }

    private static void doGenerateReport(final TcpClient c, final Scanner sc) throws IOException {
        System.out.print("Area code: ");
        printResp(c.send("GENERATE_REPORT|" + sc.nextLine().trim()));
    }

    private static void doMonthlyReport(final TcpClient c, final Scanner sc) throws IOException {
        System.out.print("Year (e.g. 2026): ");
        final String year  = sc.nextLine().trim();
        System.out.print("Month (1-12): ");
        final String month = sc.nextLine().trim();
        final String resp = c.send("MONTHLY_REPORT|" + year + "|" + month);
        printResp(resp);
        if (resp != null && resp.startsWith("OK|")) {
            System.out.println(resp.substring(3).replace("\\n", "\n"));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String readMultiline(final Scanner sc) {
        final StringBuilder sb = new StringBuilder();
        String line;
        while (!(line = sc.nextLine()).equals("--")) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(line);
        }
        return sb.toString();
    }

    private static void printResp(final String resp) {
        if (resp == null) {
            System.out.println("Connection closed by server.");
            return;
        }
        if (resp.startsWith("OK|")) {
            System.out.println("OK  " + resp.substring(3));
        } else if (resp.startsWith("ERR|")) {
            System.out.println("ERR  " + resp.substring(4));
        } else {
            System.out.println("   " + resp);
        }
    }
}
