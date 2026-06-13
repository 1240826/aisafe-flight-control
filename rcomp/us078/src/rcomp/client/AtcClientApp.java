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
        final String reg = readRegistration(sc);
        if (reg == null) return;
        final String country = pickCountry(c, sc);
        if (country == null) return;
        final String model = pickModel(c, sc);
        if (model == null) return;
        final String company = pickIata(sc, "Company IATA (e.g. TP): ");
        if (company == null) return;
        final String crew = readInt(sc, "Crew members: ", 1, 500);
        final String date = readDate(sc, "Date (yyyy-mm-dd): ");
        printResp(c.send("ADD_AIRCRAFT|" + reg + "|" + country + "|" + model
                + "|" + company + "|" + crew + "|" + date));
    }

    private static void doDecommissionAircraft(final TcpClient c, final Scanner sc) throws IOException {
        System.out.println("Choose an aircraft to decommission:");
        final String[] regAndCountry = pickAircraft(c, sc);
        if (regAndCountry == null) return;
        printResp(c.send("DECOMMISSION_AIRCRAFT|" + regAndCountry[0] + "|" + regAndCountry[1]));
    }

    private static void doCreateRoute(final TcpClient c, final Scanner sc) throws IOException {
        final String name = readRouteName(sc);
        if (name == null) return;
        final String company = pickIataFromRoutes(c, sc);
        if (company == null) return;
        final String origin = pickAirport(c, sc, "Origin");
        if (origin == null) return;
        String dest;
        while (true) {
            dest = pickAirport(c, sc, "Destination");
            if (dest == null) return;
            if (dest.equals(origin)) {
                System.out.println("Destination must be different from origin. Choose again.");
            } else {
                break;
            }
        }
        printResp(c.send("CREATE_ROUTE|" + name + "|" + company + "|" + origin + "|" + dest));
    }

    private static void doDeleteRoute(final TcpClient c, final Scanner sc) throws IOException {
        System.out.println("Choose a route to delete:");
        final String name = pickByNumber(c, sc, "LIST_ROUTES", "route");
        if (name == null) return;
        final String date = readDate(sc, "Deactivation date (yyyy-mm-dd): ");
        printResp(c.send("DELETE_ROUTE|" + name + "|" + date));
    }

    private static void doAddPilot(final TcpClient c, final Scanner sc) throws IOException {
        final String license = readLicense(sc);
        if (license == null) return;
        final String company = pickIataFromRoutes(c, sc);
        if (company == null) return;
        final String date = readDate(sc, "Certification date (yyyy-mm-dd): ");
        final String model = pickModel(c, sc);
        if (model == null) return;
        printResp(c.send("ADD_PILOT|" + license + "|" + company + "|" + date + "|" + model));
    }

    private static void doListPilots(final TcpClient c, final Scanner sc) throws IOException {
        final String company = pickIataFromRoutes(c, sc);
        if (company == null) return;
        printResp(c.send("LIST_PILOTS|" + company));
    }

    private static void doRemovePilot(final TcpClient c, final Scanner sc) throws IOException {
        final String company = pickIataFromRoutes(c, sc);
        if (company == null) return;
        System.out.println("Choose a pilot to remove:");
        final String license = pickByNumber(c, sc, "LIST_PILOTS|" + company, "pilot");
        if (license == null) return;
        printResp(c.send("REMOVE_PILOT|" + license));
    }

    private static String pickCountry(final TcpClient c, final Scanner sc) throws IOException {
        final String listResp = c.send("LIST_FLEET");
        if (listResp != null) {
            final String data = listResp.startsWith("OK|") ? listResp.substring(3) : listResp;
            final java.util.Set<String> countries = new java.util.LinkedHashSet<>();
            for (final String item : data.split(";", -1)) {
                final int open = item.indexOf('(');
                final int close = item.indexOf(')');
                if (open > 0 && close > open) {
                    countries.add(item.substring(open + 1, close).trim());
                }
            }
            if (!countries.isEmpty()) {
                final String[] arr = countries.toArray(new String[0]);
                System.out.println("\nAvailable countries:");
                for (int i = 0; i < arr.length; i++) {
                    System.out.printf("  %2d. %s%n", i + 1, arr[i]);
                }
                while (true) {
                    System.out.print("Choose country number (or type manually, 0 to cancel): ");
                    final String input = sc.nextLine().trim();
                    if (input.equals("0")) return null;
                    if (input.isEmpty()) continue;
                    if (input.matches("\\d+")) {
                        final int idx = Integer.parseInt(input) - 1;
                        if (idx >= 0 && idx < arr.length) {
                            System.out.println("Selected: " + arr[idx]);
                            return arr[idx];
                        }
                        System.out.println("Invalid number. Choose 1-" + arr.length + ".");
                    } else {
                        return input;
                    }
                }
            }
        }
        return readNonEmpty(sc, "Country: ");
    }

    private static String pickModel(final TcpClient c, final Scanner sc) throws IOException {
        final String listResp = c.send("LIST_FLEET");
        if (listResp != null) {
            final String data = listResp.startsWith("OK|") ? listResp.substring(3) : listResp;
            final java.util.Set<String> models = new java.util.LinkedHashSet<>();
            for (final String item : data.split(";", -1)) {
                final String[] parts = item.split(",");
                if (parts.length >= 2) {
                    final String model = parts[parts.length - 3].trim();
                    if (!model.isEmpty()) models.add(model);
                }
            }
            if (!models.isEmpty()) {
                final String[] arr = models.toArray(new String[0]);
                System.out.println("\nAvailable models:");
                for (int i = 0; i < arr.length; i++) {
                    System.out.printf("  %2d. %s%n", i + 1, arr[i]);
                }
                while (true) {
                    System.out.print("Choose model number (or type manually, 0 to cancel): ");
                    final String input = sc.nextLine().trim();
                    if (input.equals("0")) return null;
                    if (input.isEmpty()) continue;
                    if (input.matches("\\d+")) {
                        final int idx = Integer.parseInt(input) - 1;
                        if (idx >= 0 && idx < arr.length) {
                            System.out.println("Selected: " + arr[idx]);
                            return arr[idx];
                        }
                        System.out.println("Invalid number. Choose 1-" + arr.length + ".");
                    } else {
                        return input;
                    }
                }
            }
        }
        System.out.print("Model code (e.g. A320): ");
        return sc.nextLine().trim();
    }

    private static String pickAirport(final TcpClient c, final Scanner sc, final String label) throws IOException {
        final String listResp = c.send("LIST_ROUTES");
        if (listResp != null && listResp.startsWith("OK|")) {
            final String data = listResp.substring(3);
            final java.util.Set<String> airports = new java.util.LinkedHashSet<>();
            for (final String item : data.split(";", -1)) {
                final int arrow = item.indexOf("->");
                if (arrow >= 0) {
                    final int comma = item.lastIndexOf(',', arrow - 1);
                    if (comma >= 0) {
                        final String origin = item.substring(comma + 1, arrow).trim();
                        final String dest = item.substring(arrow + 2).trim();
                        if (!origin.isEmpty()) airports.add(origin);
                        if (!dest.isEmpty()) airports.add(dest);
                    }
                }
            }
            if (!airports.isEmpty()) {
                final String[] arr = airports.toArray(new String[0]);
                System.out.println("\nAvailable airports:");
                for (int i = 0; i < arr.length; i++) {
                    System.out.printf("  %2d. %s%n", i + 1, arr[i]);
                }
                while (true) {
                    System.out.print("Choose " + label + " airport number (or type manually, 0 to cancel): ");
                    final String input = sc.nextLine().trim();
                    if (input.equals("0")) return null;
                    if (input.isEmpty()) continue;
                    if (input.matches("\\d+")) {
                        final int idx = Integer.parseInt(input) - 1;
                        if (idx >= 0 && idx < arr.length) {
                            System.out.println("Selected: " + arr[idx]);
                            return arr[idx];
                        }
                        System.out.println("Invalid number. Choose 1-" + arr.length + ".");
                    } else {
                        return input.toUpperCase();
                    }
                }
            }
        }
        return readNonEmpty(sc, label + " airport code: ");
    }

    private static String readRegistration(final Scanner sc) {
        while (true) {
            System.out.print("Registration number (e.g. CS-TUI): ");
            final String input = sc.nextLine().trim().toUpperCase();
            if (input.isEmpty()) {
                System.out.println("Registration number cannot be empty.");
                continue;
            }
            if (!input.matches("[A-Z][A-Z0-9\\-]+")) {
                System.out.println("Invalid format: must start with a letter and contain only uppercase letters, digits, and hyphens (e.g. 'CS-TUI', 'G-BNWA', 'N12345').");
                continue;
            }
            return input;
        }
    }

    private static String[] pickAircraft(final TcpClient c, final Scanner sc) throws IOException {
        final String resp = c.send("LIST_FLEET");
        if (resp == null) {
            System.out.println("Connection closed by server.");
            return null;
        }
        String data = resp.startsWith("OK|") ? resp.substring(3) : resp;
        data = data.replaceFirst("^\\d+\\s+\\w+\\w?:\\s*", "");
        final String[] items = data.split(";", -1);
        if (items.length == 0 || (items.length == 1 && items[0].isEmpty())) {
            System.out.println("No aircraft available.");
            return null;
        }
        System.out.println("\nAvailable registrations:");
        for (int i = 0; i < items.length; i++) {
            System.out.printf("  %2d. %s%n", i + 1, items[i]);
        }
        while (true) {
            System.out.print("Choose registration number (or type manually, 0 to cancel): ");
            final String input = sc.nextLine().trim();
            if (input.equals("0")) return null;
            if (input.isEmpty()) continue;
            if (input.matches("\\d+")) {
                final int idx = Integer.parseInt(input) - 1;
                if (idx >= 0 && idx < items.length) {
                    final String selected = items[idx];
                    final String[] parts = selected.split(",", -1);
                    final String fullReg = parts[0].trim();
                    final int paren = fullReg.indexOf('(');
                    final String reg = (paren > 0 ? fullReg.substring(0, paren) : fullReg).trim();
                    final String country = (paren > 0 && fullReg.indexOf(')') > paren)
                            ? fullReg.substring(paren + 1, fullReg.indexOf(')')).trim()
                            : "";
                    System.out.println("Selected: " + reg + " (" + country + ")");
                    return new String[]{reg, country};
                }
                System.out.println("Invalid number. Choose 1-" + items.length + ".");
            } else {
                final String clean = input.indexOf('(') > 0
                        ? input.substring(0, input.indexOf('(')).trim() : input;
                System.out.print("Country for this aircraft: ");
                final String country = sc.nextLine().trim();
                if (country.isEmpty()) continue;
                return new String[]{clean, country};
            }
        }
    }

    private static String pickIata(final Scanner sc, final String label) {
        while (true) {
            System.out.print(label);
            final String input = sc.nextLine().trim().toUpperCase();
            if (input.equals("0")) return null;
            if (input.isEmpty()) {
                System.out.println("IATA code cannot be empty.");
                continue;
            }
            if (!input.matches("[A-Z]{2}")) {
                System.out.println("Company IATA code must be exactly 2 letters (A-Z), got: '" + input + "'.");
                continue;
            }
            return input;
        }
    }

    private static String pickIataFromRoutes(final TcpClient c, final Scanner sc) throws IOException {
        final String resp = c.send("LIST_ROUTES");
        if (resp != null && resp.startsWith("OK|")) {
            final java.util.Set<String> companies = new java.util.LinkedHashSet<>();
            final String data = resp.substring(3).replaceFirst("^\\d+\\s+\\w+\\w?:\\s*", "");
            for (final String item : data.split(";", -1)) {
                final String name = item.split(",", -1)[0].trim();
                if (name.length() >= 2 && name.matches("[A-Z]{2}.*")) {
                    companies.add(name.substring(0, 2).toUpperCase());
                }
            }
            if (!companies.isEmpty()) {
                final String[] arr = companies.toArray(new String[0]);
                System.out.println("\nAvailable companies:");
                for (int i = 0; i < arr.length; i++) {
                    System.out.printf("  %2d. %s%n", i + 1, arr[i]);
                }
                while (true) {
                    System.out.print("Choose company number (or type manually, 0 to cancel): ");
                    final String input = sc.nextLine().trim().toUpperCase();
                    if (input.equals("0")) return null;
                    if (input.isEmpty()) continue;
                    if (input.matches("\\d+")) {
                        final int idx = Integer.parseInt(input) - 1;
                        if (idx >= 0 && idx < arr.length) {
                            System.out.println("Selected: " + arr[idx]);
                            return arr[idx];
                        }
                        System.out.println("Invalid number. Choose 1-" + arr.length + ".");
                    } else if (input.matches("[A-Z]{2}")) {
                        return input;
                    } else {
                        System.out.println("IATA code must be exactly 2 letters (A-Z).");
                    }
                }
            }
        }
        return pickIata(sc, "Company IATA (e.g. TP): ");
    }

    private static String readRouteName(final Scanner sc) {
        while (true) {
            System.out.print("Route name (e.g. TP123): ");
            final String input = sc.nextLine().trim().toUpperCase();
            if (input.isEmpty()) {
                System.out.println("Route name cannot be empty.");
                continue;
            }
            if (!input.matches("[A-Z]{2}\\d{1,4}")) {
                System.out.println("Invalid format: must be 2 letters followed by 1-4 digits (e.g. 'TP123').");
                continue;
            }
            return input;
        }
    }

    private static String readLicense(final Scanner sc) {
        while (true) {
            System.out.print("License number (e.g. P12345): ");
            final String input = sc.nextLine().trim().toUpperCase();
            if (input.isEmpty()) {
                System.out.println("License number cannot be empty.");
                continue;
            }
            if (!input.matches("[A-Z]\\d{4,10}")) {
                System.out.println("Invalid format: must be a letter followed by 4-10 digits (e.g. 'P12345').");
                continue;
            }
            return input;
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
                    String key = selected.contains(",")
                            ? selected.substring(0, selected.indexOf(',')).trim()
                            : selected.trim();
                    final int paren = key.indexOf('(');
                    if (paren > 0) {
                        key = key.substring(0, paren).trim();
                    }
                    System.out.println("Selected: " + key);
                    return key;
                }
                System.out.println("Invalid number. Choose 1-" + items.length + ".");
            } else {
                return input;
            }
        }
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
