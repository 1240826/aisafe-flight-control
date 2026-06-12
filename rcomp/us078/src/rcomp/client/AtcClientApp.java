package rcomp.client;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class AtcClientApp {

    private static final int DEFAULT_PORT = 1078;

    private AtcClientApp() { }

    public static void main(final String[] args) {
        final String host = args.length > 0 ? args[0] : "localhost";
        final int    port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        System.out.println("==========================================");
        System.out.println("  AISafe — ATCC Client (US078)");
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
        System.out.print("Username: ");
        final String user = sc.nextLine().trim();
        System.out.print("Password: ");
        final String pass = sc.nextLine().trim();
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
            System.out.println("──── ATCC Menu ────────────────────────────");
            System.out.println(" 1. List company fleet (US072)");
            System.out.println(" 2. Add aircraft (US070)");
            System.out.println(" 3. Decommission aircraft (US071)");
            System.out.println(" 4. Create flight route (US073)");
            System.out.println(" 5. Delete flight route (US074)");
            System.out.println(" 6. Add pilot (US075)");
            System.out.println(" 7. List pilots (US076)");
            System.out.println(" 8. Remove pilot (US077)");
            System.out.println(" 9. List routes");
            System.out.println(" 0. Logout & Exit");
            System.out.print("Option: ");

            final String opt = sc.nextLine().trim();
            switch (opt) {
                case "1" -> printResp(c.send("LIST_FLEET"));
                case "2" -> doAddAircraft(c, sc);
                case "3" -> doDecommissionAircraft(c, sc);
                case "4" -> doCreateRoute(c, sc);
                case "5" -> doDeleteRoute(c, sc);
                case "6" -> doAddPilot(c, sc);
                case "7" -> doListPilots(c, sc);
                case "8" -> doRemovePilot(c, sc);
                case "9" -> printResp(c.send("LIST_ROUTES"));
                case "0" -> { System.out.println("Goodbye."); return; }
                default  -> System.out.println("Invalid option '" + opt + "'. Choose 0-9.");
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

    private static String readDate(final Scanner sc, final String label) {
        while (true) {
            System.out.print(label);
            final String input = sc.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("Date cannot be empty. Use format yyyy-MM-dd (e.g. 2026-06-01).");
                continue;
            }
            try {
                LocalDate.parse(input);
                return input;
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid date '" + input + "'. Expected format: yyyy-MM-dd (e.g. 2026-06-01).");
            }
        }
    }

    private static void doAddAircraft(final TcpClient c, final Scanner sc) throws IOException {
        final String reg = readNonEmpty(sc, "Registration number: ");
        final String country = readNonEmpty(sc, "Country: ");
        System.out.print("Model code (e.g. A320): ");
        final String model = sc.nextLine().trim();
        final String company = readNonEmpty(sc, "Company IATA (e.g. TP): ");
        final String crew = readInt(sc, "Crew members: ", 1, 500);
        final String date = readDate(sc, "Date (yyyy-mm-dd): ");
        printResp(c.send("ADD_AIRCRAFT|" + reg + "|" + country + "|" + model
                + "|" + company + "|" + crew + "|" + date));
    }

    private static void doDecommissionAircraft(final TcpClient c, final Scanner sc) throws IOException {
        System.out.println("Choose an aircraft to decommission (or type registration manually):");
        final String reg = pickFromList(c, sc, "LIST_FLEET", "Registration number");
        if (reg == null) return;
        final String country = readNonEmpty(sc, "Country: ");
        printResp(c.send("DECOMMISSION_AIRCRAFT|" + reg + "|" + country));
    }

    private static void doCreateRoute(final TcpClient c, final Scanner sc) throws IOException {
        final String name = readNonEmpty(sc, "Route name: ");
        final String company = readNonEmpty(sc, "Company IATA: ");
        final String origin = readNonEmpty(sc, "Origin airport code: ");
        final String dest = readNonEmpty(sc, "Destination airport code: ");
        printResp(c.send("CREATE_ROUTE|" + name + "|" + company + "|" + origin + "|" + dest));
    }

    private static void doDeleteRoute(final TcpClient c, final Scanner sc) throws IOException {
        System.out.println("Choose a route to delete (or type name manually, 0 to cancel):");
        final String name = pickFromList(c, sc, "LIST_ROUTES", "Route name");
        if (name == null) return;
        final String date = readDate(sc, "Deactivation date (yyyy-mm-dd): ");
        printResp(c.send("DELETE_ROUTE|" + name + "|" + date));
    }

    private static void doAddPilot(final TcpClient c, final Scanner sc) throws IOException {
        final String license = readNonEmpty(sc, "License number: ");
        final String company = readNonEmpty(sc, "Company IATA: ");
        final String date = readDate(sc, "Certification date (yyyy-mm-dd): ");
        System.out.print("Certified models (comma-separated, e.g. A320,B738): ");
        final String models = sc.nextLine().trim();
        printResp(c.send("ADD_PILOT|" + license + "|" + company + "|" + date + "|" + models));
    }

    private static void doListPilots(final TcpClient c, final Scanner sc) throws IOException {
        final String company = readNonEmpty(sc, "Company IATA: ");
        printResp(c.send("LIST_PILOTS|" + company));
    }

    private static void doRemovePilot(final TcpClient c, final Scanner sc) throws IOException {
        final String company = readNonEmpty(sc, "Company IATA: ");
        System.out.println("Choose a pilot to remove (or type license manually, 0 to cancel):");
        final String license = pickFromList(c, sc, "LIST_PILOTS|" + company, "License number");
        if (license == null) return;
        printResp(c.send("REMOVE_PILOT|" + license));
    }

    private static String pickFromList(final TcpClient c, final Scanner sc, final String listCommand, final String fieldName) throws IOException {
        final String listResp = c.send(listCommand);
        if (listResp != null) {
            final String display = listResp.startsWith("OK|") ? listResp.substring(3) : listResp;
            System.out.println("Available: " + display);
        }
        while (true) {
            System.out.print(fieldName + " (or 0 to cancel): ");
            final String input = sc.nextLine().trim();
            if (input.equals("0")) return null;
            if (!input.isEmpty()) return input;
            System.out.println(fieldName + " cannot be empty.");
        }
    }

    private static boolean isInt(final String s, final String field) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (final NumberFormatException e) {
            System.out.println("Invalid " + field + ": must be a whole number.");
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
