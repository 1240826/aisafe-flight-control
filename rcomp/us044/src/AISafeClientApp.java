package rcomp.client;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

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
        String selectedArea = selectArea(c, sc);
        while (true) {
            System.out.println();
            System.out.println("──── Weather Menu ────────────────────────");
            System.out.println(" 1. Register weather data");
            System.out.println(" 2. Import bulk weather data (CSV)");
            System.out.println(" 3. Consult weather data");
            System.out.println(" 4. List air control areas");
            System.out.println(" 5. Change selected area");
            System.out.println(" 0. Logout & Exit");
            if (selectedArea != null) System.out.println(" [Selected area: " + selectedArea + "]");
            System.out.print("Option: ");

            final String opt = sc.nextLine().trim();
            switch (opt) {
                case "1" -> doRegisterWeather(c, sc, selectedArea);
                case "2" -> doImportWeather(c, sc, selectedArea);
                case "3" -> doConsultWeather(c, sc, selectedArea);
                case "4" -> printResp(c.send("LIST_AREAS"));
                case "5" -> selectedArea = selectArea(c, sc);
                case "0" -> { System.out.println("Goodbye."); return; }
                default  -> System.out.println("Invalid option '" + opt + "'. Choose 0-5.");
            }
        }
    }

    private static String selectArea(final TcpClient c, final Scanner sc) throws IOException {
        while (true) {
            System.out.println("\n──── Area Selection ───────────────────────");
            System.out.println(" Current area: " + (c.getSelectedArea() != null ? c.getSelectedArea() : "None"));
            System.out.println(" 1. Select area from list");
            System.out.println(" 2. Enter area code manually");
            System.out.println(" 3. Back to main menu");
            System.out.print("Option: ");

            final String option = sc.nextLine().trim();
            switch (option) {
                case "1" -> {
                    final String areas = c.send("LIST_AREAS");
                    if (areas == null) {
                        System.out.println("Failed to retrieve areas from server.");
                        break;
                    }
                    final String[] areaLines = areas.split("\\n");
                    System.out.println("\nAvailable Areas:");
                    for (int i = 0; i < areaLines.length; i++) {
                        System.out.println((i + 1) + ". " + areaLines[i]);
                    }
                    while (true) {
                        System.out.print("Enter area number (or empty to go back): ");
                        final String areaNum = sc.nextLine().trim();
                        if (areaNum.isEmpty()) break;
                        try {
                            final int index = Integer.parseInt(areaNum) - 1;
                            if (index >= 0 && index < areaLines.length) {
                                final String selected = areaLines[index];
                                System.out.println("Selected area: " + selected);
                                c.setSelectedArea(selected);
                                return selected;
                            } else {
                                System.out.println("Invalid area number. Choose between 1 and " + areaLines.length + ".");
                            }
                        } catch (final NumberFormatException e) {
                            System.out.println("Invalid number '" + areaNum + "'. Enter a number between 1 and " + areaLines.length + ".");
                        }
                    }
                }
                case "2" -> {
                    System.out.print("Enter area code: ");
                    final String area = sc.nextLine().trim();
                    if (area.isEmpty()) {
                        System.out.println("Area code cannot be empty.");
                    } else {
                        System.out.println("Selected area: " + area);
                        c.setSelectedArea(area);
                        return area;
                    }
                }
                case "3" -> { System.out.println("Returning to main menu..."); return c.getSelectedArea(); }
                default -> System.out.println("Invalid option '" + option + "'. Choose 1, 2, or 3.");
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

    private static String readDouble(final Scanner sc, final String label) {
        while (true) {
            System.out.print(label);
            final String input = sc.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println(label.replace(":", "").trim() + " cannot be empty.");
                continue;
            }
            try {
                Double.parseDouble(input);
                return input;
            } catch (final NumberFormatException e) {
                System.out.println("Invalid number '" + input + "'. Expected a decimal number (e.g. 12.5).");
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

    private static String readDateTime(final Scanner sc, final String label) {
        while (true) {
            System.out.print(label);
            final String input = sc.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("DateTime cannot be empty. Use ISO format (e.g. 2026-06-01T14:30:00).");
                continue;
            }
            try {
                LocalDateTime.parse(input);
                return input;
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid datetime '" + input + "'. Expected format: yyyy-MM-ddTHH:mm:ss (e.g. 2026-06-01T14:30:00).");
            }
        }
    }

    private static void doRegisterWeather(final TcpClient c, final Scanner sc, final String areaOverride) throws IOException {
        final String area = areaOverride != null ? areaOverride : c.getSelectedArea();
        if (area == null || area.isEmpty()) {
            System.out.println("No area selected. Please select an area first (option 5).");
            return;
        }
        System.out.println("Using area: " + area);
        final String lat   = readDouble(sc, "Latitude: ");
        final String lon   = readDouble(sc, "Longitude: ");
        final String alt   = readDouble(sc, "Altitude (m): ");
        final String speed = readDouble(sc, "Wind speed (knots): ");
        final String dir   = readDouble(sc, "Wind direction (deg): ");
        final String temp  = readDouble(sc, "Temperature (°C): ");
        final String prov  = readNonEmpty(sc, "Provider: ");
        final String dt    = readDateTime(sc, "DateTime (ISO format e.g. 2026-06-01T14:30:00): ");

        printResp(c.send("REGISTER_WEATHER|" + area + "|" + lat + "|" + lon + "|"
                + alt + "|" + speed + "|" + dir + "|" + temp + "|" + prov + "|" + dt));
    }

    private static void doImportWeather(final TcpClient c, final Scanner sc, final String areaOverride) throws IOException {
        final String area = areaOverride != null ? areaOverride : c.getSelectedArea();
        if (area == null || area.isEmpty()) {
            System.out.println("No area selected. Please select an area first (option 5).");
            return;
        }
        System.out.println("Using area: " + area);
        while (true) {
            System.out.println("\n──── Bulk Import ───────────────────────────");
            System.out.println(" 1. Enter CSV data manually");
            System.out.println(" 2. Import from CSV file");
            System.out.println(" 0. Cancel");
            System.out.print("Option: ");
            final String opt = sc.nextLine().trim();
            if (opt.equals("0")) { System.out.println("Cancelled."); return; }
            if (opt.equals("1")) {
                System.out.println("CSV rows (lat,lon,alt,speed,dir,temp,provider[,datetime])");
                System.out.println("Separate with  ;  — blank line to finish:");
                final StringBuilder csv = new StringBuilder();
                String line;
                while (!(line = sc.nextLine()).isBlank()) {
                    if (csv.length() > 0) csv.append(";");
                    csv.append(line.trim());
                }
                if (csv.isEmpty()) {
                    System.out.println("No data entered. Operation cancelled.");
                    return;
                }
                printResp(c.send("IMPORT_WEATHER|" + area + "|" + csv));
                return;
            }
            if (opt.equals("2")) {
                final java.nio.file.Path curDir = java.nio.file.Paths.get(".").toAbsolutePath().normalize();
                System.out.println("CSV files in " + curDir + ":");
                final java.util.List<java.nio.file.Path> csvFiles;
                try (final var stream = java.nio.file.Files.list(curDir)) {
                    csvFiles = stream
                            .filter(f -> f.toString().toLowerCase().endsWith(".csv"))
                            .sorted()
                            .collect(java.util.stream.Collectors.toList());
                }
                if (csvFiles.isEmpty()) {
                    System.out.println("  (no .csv files found)");
                } else {
                    for (int i = 0; i < csvFiles.size(); i++) {
                        System.out.printf("  %d. %s%n", i + 1, csvFiles.get(i).getFileName());
                    }
                }
                System.out.println("  (or type a path manually, or 0 to cancel)");
                System.out.print("Choose file: ");
                final String input = sc.nextLine().trim();
                if (input.equals("0")) continue;
                final java.nio.file.Path chosen;
                if (input.matches("\\d+")) {
                    final int idx = Integer.parseInt(input) - 1;
                    if (idx >= 0 && idx < csvFiles.size()) {
                        chosen = csvFiles.get(idx);
                    } else {
                        System.out.println("Invalid selection.");
                        continue;
                    }
                } else {
                    chosen = java.nio.file.Paths.get(input);
                }
                if (!java.nio.file.Files.exists(chosen)) {
                    System.out.println("File not found: " + chosen + ".");
                    continue;
                }
                try {
                    final String csv = java.nio.file.Files.readString(chosen);
                    printResp(c.send("IMPORT_WEATHER|" + area + "|" + csv));
                    return;
                } catch (final java.io.IOException e) {
                    System.out.println("Error reading file: " + e.getMessage() + ".");
                }
            }
        }
    }

    private static void doConsultWeather(final TcpClient c, final Scanner sc, final String areaOverride) throws IOException {
        final String area = areaOverride != null ? areaOverride : c.getSelectedArea();
        if (area == null || area.isEmpty()) {
            System.out.println("No area selected. Please select an area first (option 5).");
            return;
        }
        System.out.println("Using area: " + area);
        final String date = readDate(sc, "Date (e.g. 2026-06-01): ");
        printResp(c.send("CONSULT_WEATHER|" + area + "|" + date));
    }

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
