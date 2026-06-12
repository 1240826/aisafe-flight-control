package rcomp.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

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

    private static boolean login(final TcpClient c, final Scanner sc) throws IOException {
        System.out.print("Username: "); final String user = sc.nextLine().trim();
        System.out.print("Password: "); final String pass = sc.nextLine().trim();
        final String resp = c.send("AUTH|" + user + "|" + pass);
        System.out.println("Server: " + resp);
        if (resp == null) {
            System.out.println("Connection closed by server during authentication.");
            return false;
        }
        if (resp.startsWith("AUTH_OK")) {
            return true;
        }
        if (resp.startsWith("AUTH_FAIL")) {
            System.out.println("Authentication failed: " + resp.substring(9));
            return false;
        }
        System.out.println("Unexpected server response: " + resp);
        return false;
    }

    private static void mainLoop(final TcpClient c, final Scanner sc) throws IOException {
        while (true) {
            System.out.println();
            System.out.println("──── Pilot Menu ────────────────────────────");
            System.out.println(" 1. List company fleet (US072)");
            System.out.println(" 2. Create flight plan (US080)");
            System.out.println(" 3. Import flight plan from file (US121)");
            System.out.println(" 4. Validate flight plan (US085)");
            System.out.println(" 5. Generate simulation report (US111)");
            System.out.println(" 6. Monthly report (US112)");
            System.out.println(" 7. List flights");
            System.out.println(" 8. List routes");
            System.out.println(" 0. Logout & Exit");
            System.out.print("Option: ");

            final String opt = sc.nextLine().trim();
            switch (opt) {
                case "1" -> printResp(c.send("LIST_FLEET"));
                case "2" -> doCreateFlightPlan(c, sc);
                case "3" -> doImportFlightPlan(c, sc);
                case "4" -> doValidateFlightPlan(c, sc);
                case "5" -> doGenerateReport(c, sc);
                case "6" -> doMonthlyReport(c, sc);
                case "7" -> printResp(c.send("LIST_FLIGHTS"));
                case "8" -> printResp(c.send("LIST_ROUTES"));
                case "0" -> { System.out.println("Goodbye."); return; }
                default  -> System.out.println("Invalid option '" + opt + "'. Choose 0-8.");
            }
        }
    }

    private static String readNonEmpty(final Scanner sc, final String label) {
        while (true) {
            System.out.print(label);
            final String input = sc.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println(label.replace(":", "").trim() + " cannot be empty. Please try again.");
        }
    }

    private static String readInt(final Scanner sc, final String label, final int min, final int max) {
        while (true) {
            System.out.print(label);
            final String input = sc.nextLine().trim();
            try {
                final int val = Integer.parseInt(input);
                if (val >= min && val <= max) return input;
                System.out.println("Value must be between " + min + " and " + max + ". Got: " + val);
            } catch (final NumberFormatException e) {
                System.out.println("Invalid number '" + input + "'. Expected a whole number between " + min + " and " + max + ".");
            }
        }
    }

    private static void doCreateFlightPlan(final TcpClient c, final Scanner sc) throws IOException {
        final String flightId = readNonEmpty(sc, "Flight ID: ");
        System.out.println("DSL content (end with a line containing only '--'):");
        final String dsl = readMultiline(sc);
        if (dsl.isEmpty()) {
            System.out.println("No DSL content provided. Operation cancelled.");
            return;
        }
        printResp(c.send("CREATE_FLIGHT_PLAN|" + flightId + "|" + dsl));
    }

    private static void doImportFlightPlan(final TcpClient c, final Scanner sc) throws IOException {
        final String flightId = readNonEmpty(sc, "Flight ID: ");
        while (true) {
            final java.nio.file.Path curDir = Paths.get(".").toAbsolutePath().normalize();
            System.out.println("DSL files in " + curDir + ":");
            final java.util.List<java.nio.file.Path> dslFiles;
            try (final var stream = java.nio.file.Files.list(curDir)) {
                dslFiles = stream
                        .filter(f -> f.toString().toLowerCase().endsWith(".dsl"))
                        .sorted()
                        .collect(java.util.stream.Collectors.toList());
            }
            if (dslFiles.isEmpty()) {
                System.out.println("  (no .dsl files found)");
            } else {
                for (int i = 0; i < dslFiles.size(); i++) {
                    System.out.printf("  %d. %s%n", i + 1, dslFiles.get(i).getFileName());
                }
            }
            System.out.println("  (or type a path manually, or 0 to cancel)");
            System.out.print("Choose file: ");
            final String input = sc.nextLine().trim();
            if (input.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }
            final java.nio.file.Path chosen;
            if (input.matches("\\d+")) {
                final int idx = Integer.parseInt(input) - 1;
                if (idx >= 0 && idx < dslFiles.size()) {
                    chosen = dslFiles.get(idx);
                } else {
                    System.out.println("Invalid selection. Try again.");
                    continue;
                }
            } else {
                chosen = Paths.get(input);
            }
            if (!Files.exists(chosen)) {
                System.out.println("File not found: " + chosen + ". Try again.");
                continue;
            }
            try {
                final String dsl = Files.readString(chosen);
                printResp(c.send("IMPORT_FLIGHT_PLAN|" + flightId + "|" + dsl));
                return;
            } catch (final IOException e) {
                System.out.println("Error reading file: " + e.getMessage() + ". Try again.");
            }
        }
    }

    private static void doValidateFlightPlan(final TcpClient c, final Scanner sc) throws IOException {
        final String id = readNonEmpty(sc, "Flight Plan ID: ");
        printResp(c.send("VALIDATE_FLIGHT_PLAN|" + id));
    }

    private static void doGenerateReport(final TcpClient c, final Scanner sc) throws IOException {
        final String area = readNonEmpty(sc, "Area code: ");
        printResp(c.send("GENERATE_REPORT|" + area));
    }

    private static void doMonthlyReport(final TcpClient c, final Scanner sc) throws IOException {
        final String year = readInt(sc, "Year (e.g. 2026): ", 2000, 2100);
        final String month = readInt(sc, "Month (1-12): ", 1, 12);
        final String resp = c.send("MONTHLY_REPORT|" + year + "|" + month);
        if (resp == null) {
            System.out.println("Connection closed by server.");
            return;
        }
        if (resp.startsWith("OK|")) {
            System.out.println(resp.substring(3).replace("\\n", "\n"));
        } else if (resp.startsWith("ERR|")) {
            System.out.println("ERR  " + resp.substring(4));
        } else {
            System.out.println(resp.replace("\\n", "\n"));
        }
    }

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
        } else if (resp.startsWith("AUTH_FAIL")) {
            System.out.println("AUTH FAIL  " + resp.substring(9));
        } else {
            System.out.println("   " + resp);
        }
    }
}
