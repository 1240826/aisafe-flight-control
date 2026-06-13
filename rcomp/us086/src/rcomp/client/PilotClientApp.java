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
        final String flightId = pickFlight(c, sc);
        if (flightId == null) return;
        System.out.println("DSL content (end with a line containing only '--'):");
        final String dsl = readMultiline(sc);
        if (dsl.isEmpty()) {
            System.out.println("No DSL content provided. Operation cancelled.");
            return;
        }
        printResp(c.send("CREATE_FLIGHT_PLAN|" + flightId + "|" + encodeDsl(dsl)));
    }

    private static void doImportFlightPlan(final TcpClient c, final Scanner sc) throws IOException {
        System.out.println();
        System.out.println(" 1. Pick a .flightplan file");
        System.out.println(" 2. Enter DSL content manually");
        System.out.print("Option (or 0 to cancel): ");
        final String opt = sc.nextLine().trim();
        if (opt.equals("0")) return;
        String dsl;
        if (opt.equals("2")) {
            System.out.println("DSL content (end with a line containing only '--'):");
            dsl = readMultiline(sc);
        } else {
            dsl = pickFile(sc, ".flightplan");
        }
        if (dsl == null || dsl.isEmpty()) {
            System.out.println("No DSL content provided. Operation cancelled.");
            return;
        }
        final String flightId = extractFlightId(dsl);
        if (flightId == null) {
            System.out.println("Could not extract flight ID from the DSL content. Make sure it starts with 'flight XXXXX :'.");
            return;
        }
        System.out.println("Extracted flight ID: " + flightId);
        printResp(c.send("IMPORT_FLIGHT_PLAN|" + flightId + "|" + encodeDsl(dsl)));
    }

    private static void doValidateFlightPlan(final TcpClient c, final Scanner sc) throws IOException {
        System.out.println("Choose a flight plan to validate:");
        final String id = pickByNumber(c, sc, "LIST_FLIGHTS", "flight plan");
        if (id == null) return;
        printResp(c.send("VALIDATE_FLIGHT_PLAN|" + id));
    }

    private static void doGenerateReport(final TcpClient c, final Scanner sc) throws IOException {
        System.out.println("Tip: use option 8 (List routes) to see available routes.");
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

    private static String pickFlight(final TcpClient c, final Scanner sc) throws IOException {
        System.out.println();
        System.out.println(" 1. Type flight ID manually");
        System.out.println(" 2. Select from existing flights");
        System.out.print("Flight ID option (or 0 to cancel): ");
        final String opt = sc.nextLine().trim();
        if (opt.equals("0")) return null;
        if (opt.equals("2")) {
            final String id = pickByNumber(c, sc, "LIST_FLIGHTS", "flight");
            if (id != null) return id;
        }
        return readNonEmpty(sc, "Flight ID: ");
    }

    private static String pickFile(final Scanner sc, final String extension) {
        final java.util.List<java.nio.file.Path> files = new java.util.ArrayList<>();
        final java.nio.file.Path curDir = Paths.get(".").toAbsolutePath().normalize();
        try (final var stream = java.nio.file.Files.list(curDir)) {
            stream.filter(f -> f.toString().toLowerCase().endsWith(extension))
                    .sorted().forEach(files::add);
        } catch (final IOException ignored) { }
        final java.nio.file.Path examplesDir = Paths.get(
                curDir.toString().replace("rcomp" + java.io.File.separator + "us086", ""),
                "aisafe.dsl", "src", "main", "resources", "examples");
        if (Files.isDirectory(examplesDir)) {
            try (final var stream = java.nio.file.Files.list(examplesDir)) {
                stream.filter(f -> f.toString().toLowerCase().endsWith(extension))
                        .sorted().forEach(files::add);
            } catch (final IOException ignored) { }
        }
        System.out.println("\nAvailable " + extension + " files:");
        if (files.isEmpty()) {
            System.out.println("  (no " + extension + " files found)");
        } else {
            for (int i = 0; i < files.size(); i++) {
                System.out.printf("  %2d. %s%n", i + 1, files.get(i).getFileName());
            }
        }
        while (true) {
            System.out.print("Choose file number (or type path manually, 0 to cancel): ");
            final String input = sc.nextLine().trim();
            if (input.equals("0")) return null;
            if (input.isEmpty()) continue;
            final java.nio.file.Path chosen;
            if (input.matches("\\d+")) {
                final int idx = Integer.parseInt(input) - 1;
                if (idx >= 0 && idx < files.size()) {
                    chosen = files.get(idx);
                } else {
                    System.out.println("Invalid selection. Choose 1-" + files.size() + ".");
                    continue;
                }
            } else {
                chosen = Paths.get(input);
            }
            if (!Files.exists(chosen)) {
                System.out.println("File not found: " + chosen + ".");
                continue;
            }
            try {
                return Files.readString(chosen);
            } catch (final IOException e) {
                System.out.println("Error reading file: " + e.getMessage() + ".");
            }
        }
    }

    private static String pickByNumber(final TcpClient c, final Scanner sc,
                                       final String listCommand, final String itemLabel) throws IOException {
        final String resp = c.send(listCommand);
        if (resp == null) {
            System.out.println("Connection closed by server.");
            return null;
        }
        String data = resp.startsWith("OK|") ? resp.substring(3) : resp;
        data = data.replaceFirst("^\\d+\\s+\\w+\\w?:\\s*", "");
        final String[] items = data.split(";", -1);
        if (items.length == 0 || (items.length == 1 && items[0].isEmpty())) {
            System.out.println("No " + itemLabel + "s available.");
            return null;
        }
        System.out.println("\nAvailable " + itemLabel + "s:");
        for (int i = 0; i < items.length; i++) {
            System.out.printf("  %2d. %s%n", i + 1, items[i]);
        }
        while (true) {
            System.out.print("Choose " + itemLabel + " number (or type manually, 0 to cancel): ");
            final String input = sc.nextLine().trim();
            if (input.equals("0")) return null;
            if (input.isEmpty()) continue;
            if (input.matches("\\d+")) {
                final int idx = Integer.parseInt(input) - 1;
                if (idx >= 0 && idx < items.length) {
                    final String selected = items[idx];
                    final String key = selected.contains(",")
                            ? selected.substring(0, selected.indexOf(',')).trim()
                            : selected.trim();
                    System.out.println("Selected: " + key);
                    return key;
                }
                System.out.println("Invalid number. Choose 1-" + items.length + ".");
            } else {
                return input;
            }
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

    private static String extractFlightId(final String dsl) {
        final java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("^flight\\s+(\\S+)\\s*:",
                        java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.MULTILINE)
                        .matcher(dsl);
        return m.find() ? m.group(1) : null;
    }

    private static String encodeDsl(final String dsl) {
        return dsl.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\\n");
    }

    private static void printResp(final String resp) {
        if (resp == null) {
            System.out.println("Connection closed by server.");
            return;
        }
        final String display = resp.replace("\\n", "\n");
        if (display.startsWith("OK|")) {
            System.out.println("OK  " + display.substring(3));
        } else if (display.startsWith("ERR|")) {
            System.out.println("ERR  " + display.substring(4));
        } else if (display.startsWith("AUTH_FAIL")) {
            System.out.println("AUTH FAIL  " + display.substring(9));
        } else {
            System.out.println("   " + display);
        }
    }
}
