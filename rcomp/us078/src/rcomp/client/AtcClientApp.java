package rcomp.client;

import java.io.IOException;
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

	// ── Auth

	private static boolean login(final TcpClient c, final Scanner sc) throws IOException {
		System.out.print("Username: ");
		final String user = sc.nextLine().trim();
		System.out.print("Password: ");
		final String pass = sc.nextLine().trim();
		final String resp = c.send("AUTH|" + user + "|" + pass);
		System.out.println("Server: " + resp);
		return resp != null && resp.startsWith("AUTH_OK");
	}

	// ── Menu

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

			switch (sc.nextLine().trim()) {
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
				default  -> System.out.println("Invalid option.");
			}
		}
	}

	// ── Operations

	private static void doAddAircraft(final TcpClient c, final Scanner sc) throws IOException {
		System.out.print("Registration number: ");
		final String reg = sc.nextLine().trim();
		System.out.print("Country: ");
		final String country = sc.nextLine().trim();
		System.out.print("Model code (e.g. A320): ");
		final String model = sc.nextLine().trim();
		System.out.print("Company IATA (e.g. TP): ");
		final String company = sc.nextLine().trim();
		System.out.print("Crew members: ");
		final String crew = sc.nextLine().trim();
		System.out.print("Date (yyyy-mm-dd): ");
		final String date = sc.nextLine().trim();
		printResp(c.send("ADD_AIRCRAFT|" + reg + "|" + country + "|" + model
				+ "|" + company + "|" + crew + "|" + date));
	}

	private static void doDecommissionAircraft(final TcpClient c, final Scanner sc) throws IOException {
		System.out.print("Registration number: ");
		final String reg = sc.nextLine().trim();
		System.out.print("Country: ");
		final String country = sc.nextLine().trim();
		printResp(c.send("DECOMMISSION_AIRCRAFT|" + reg + "|" + country));
	}

	private static void doCreateRoute(final TcpClient c, final Scanner sc) throws IOException {
		System.out.print("Route name: ");
		final String name = sc.nextLine().trim();
		System.out.print("Company IATA: ");
		final String company = sc.nextLine().trim();
		System.out.print("Origin airport code: ");
		final String origin = sc.nextLine().trim();
		System.out.print("Destination airport code: ");
		final String dest = sc.nextLine().trim();
		printResp(c.send("CREATE_ROUTE|" + name + "|" + company + "|" + origin + "|" + dest));
	}

	private static void doDeleteRoute(final TcpClient c, final Scanner sc) throws IOException {
		System.out.print("Route name: ");
		final String name = sc.nextLine().trim();
		System.out.print("Deactivation date (yyyy-mm-dd): ");
		final String date = sc.nextLine().trim();
		printResp(c.send("DELETE_ROUTE|" + name + "|" + date));
	}

	private static void doAddPilot(final TcpClient c, final Scanner sc) throws IOException {
		System.out.print("License number: ");
		final String license = sc.nextLine().trim();
		System.out.print("Company IATA: ");
		final String company = sc.nextLine().trim();
		System.out.print("Certification date (yyyy-mm-dd): ");
		final String date = sc.nextLine().trim();
		System.out.print("Certified models (comma-separated, e.g. A320,B738): ");
		final String models = sc.nextLine().trim();
		printResp(c.send("ADD_PILOT|" + license + "|" + company + "|" + date + "|" + models));
	}

	private static void doListPilots(final TcpClient c, final Scanner sc) throws IOException {
		System.out.print("Company IATA: ");
		printResp(c.send("LIST_PILOTS|" + sc.nextLine().trim()));
	}

	private static void doRemovePilot(final TcpClient c, final Scanner sc) throws IOException {
		System.out.print("License number: ");
		printResp(c.send("REMOVE_PILOT|" + sc.nextLine().trim()));
	}

	// ── Helpers

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