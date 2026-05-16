package eapli.aisafe.ui.collaborator;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.collaborator.application.AddCollaboratorController;
import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * UI for US061 - Add Customer's Collaborator.
 * Allows adding ATC Collaborator, Flight Control Operator, or Weather Person.
 */
@SuppressWarnings("squid:S106")
public class AddCollaboratorUI extends AbstractUI {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final AddCollaboratorController controller = new AddCollaboratorController();

    @Override
    protected boolean doShow() {
        System.out.println("Collaborator types:");
        System.out.println("  1. ATC Collaborator (Air Traffic Controller)");
        System.out.println("  2. Flight Control Operator");
        System.out.println("  3. Weather Person");

        // Collaborator type selection: bounds check with retry
        int type;
        do {
            type = Console.readInteger("Select type (1-3)");
            if (type < 1 || type > 3) {
                System.out.println("  [!] Invalid selection. Please enter a number between 1 and 3.");
            }
        } while (type < 1 || type > 3);

        // Username: non-blank with retry
        String username;
        do {
            username = Console.readLine("Username (for system login)").trim();
            if (username.isBlank()) {
                System.out.println("  [!] Username cannot be blank. Please try again.");
            }
        } while (username.isBlank());

        // Password: full policy check (8+ chars, 1 digit, 1 capital) with retry
        String password;
        do {
            System.out.println("  Password: min. 8 characters, at least 1 digit and 1 capital letter.");
            password = Console.readLine("Password");
            boolean valid = password.length() >= 8
                    && password.chars().anyMatch(Character::isDigit)
                    && password.chars().anyMatch(Character::isUpperCase);
            if (!valid) {
                System.out.println("  [!] Password does not meet requirements. Please try again.");
                password = null;
            }
        } while (password == null);

        // First Name: non-blank with retry
        String firstName;
        do {
            firstName = Console.readLine("First Name").trim();
            if (firstName.isBlank()) {
                System.out.println("  [!] First Name cannot be blank. Please try again.");
            }
        } while (firstName.isBlank());

        // Last Name: non-blank with retry
        String lastName;
        do {
            lastName = Console.readLine("Last Name").trim();
            if (lastName.isBlank()) {
                System.out.println("  [!] Last Name cannot be blank. Please try again.");
            }
        } while (lastName.isBlank());

        // Email: must match local@domain.tld format
        String email = null;
        while (email == null) {
            final String emailInput = Console.readLine("Email (e.g. user@example.com)").trim();
            if (!emailInput.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
                System.out.println("  [!] Please enter a valid email address (e.g. user@example.com).");
            } else {
                email = emailInput;
            }
        }

        // Full Name / Display Name: must contain at least one letter
        String name = null;
        while (name == null) {
            final String nameInput = Console.readLine("Full Name / Display Name (e.g. 'João Silva')").trim();
            if (nameInput.isBlank()) {
                System.out.println("  [!] Full Name / Display Name cannot be blank. Please try again.");
            } else if (!nameInput.matches(".*\\p{L}.*")) {
                System.out.println("  [!] Name must contain at least one letter. Please try again.");
            } else {
                name = nameInput;
            }
        }

        // Position: must contain at least one letter
        String position = null;
        while (position == null) {
            final String posInput = Console.readLine("Position / Job Title (e.g. 'ATC Controller')").trim();
            if (posInput.isBlank()) {
                System.out.println("  [!] Position / Job Title cannot be blank. Please try again.");
            } else if (!posInput.matches(".*\\p{L}.*")) {
                System.out.println("  [!] Position must contain at least one letter. Please try again.");
            } else {
                position = posInput;
            }
        }

        // Security Clearance Expiry Date: parse with retry on DateTimeParseException
        LocalDate clearance = null;
        while (clearance == null) {
            try {
                String raw = Console.readLine("Security Clearance Expiry Date (yyyy-MM-dd)").trim();
                clearance = LocalDate.parse(raw, DATE_FMT);
            } catch (DateTimeParseException e) {
                System.out.println("  [!] Invalid date format. Please use yyyy-MM-dd (e.g. 2027-12-31).");
            }
        }

        // Skills Assessment Date: parse with retry on DateTimeParseException
        LocalDate assessment = null;
        while (assessment == null) {
            try {
                String raw = Console.readLine("Skills Assessment Date (yyyy-MM-dd)").trim();
                assessment = LocalDate.parse(raw, DATE_FMT);
            } catch (DateTimeParseException e) {
                System.out.println("  [!] Invalid date format. Please use yyyy-MM-dd (e.g. 2027-12-31).");
            }
        }

        try {
            switch (type) {
                case 1: {
                    // Select company from numbered list
                    final List<AirTransportCompany> companies = new ArrayList<>();
                    controller.allCompanies().forEach(companies::add);
                    if (companies.isEmpty()) {
                        System.out.println("  [!] No companies available. Please register some first.");
                        return false;
                    }
                    System.out.println("\nAvailable Companies:");
                    for (int i = 0; i < companies.size(); i++) {
                        final AirTransportCompany c = companies.get(i);
                        System.out.printf("  %d. %s/%s - %s%n", i + 1, c.iata(), c.icao(), c.name());
                    }
                    int companyIdx;
                    do {
                        companyIdx = Console.readInteger("Select (1-" + companies.size() + ")");
                        if (companyIdx < 1 || companyIdx > companies.size()) {
                            System.out.println("  [!] Please enter a number between 1 and " + companies.size() + ".");
                        }
                    } while (companyIdx < 1 || companyIdx > companies.size());
                    final String iata = companies.get(companyIdx - 1).iata().toString();
                    controller.addATCCollaborator(username, password, firstName, lastName, email,
                            name, position, clearance, assessment, iata);
                    break;
                }
                case 2: {
                    // Select air control area from numbered list
                    final List<AirControlArea> areas = new ArrayList<>();
                    controller.allAirControlAreas().forEach(areas::add);
                    if (areas.isEmpty()) {
                        System.out.println("  [!] No Air Control Areas available. Please register some first.");
                        return false;
                    }
                    System.out.println("\nAvailable Air Control Areas:");
                    for (int i = 0; i < areas.size(); i++) {
                        final AirControlArea a = areas.get(i);
                        System.out.printf("  %d. %s - %s [lat %.2f to %.2f x lon %.2f to %.2f]%n",
                                i + 1, a.code(), a.name(),
                                a.minLat(), a.maxLat(), a.minLon(), a.maxLon());
                    }
                    int areaIdx;
                    do {
                        areaIdx = Console.readInteger("Select (1-" + areas.size() + ")");
                        if (areaIdx < 1 || areaIdx > areas.size()) {
                            System.out.println("  [!] Please enter a number between 1 and " + areas.size() + ".");
                        }
                    } while (areaIdx < 1 || areaIdx > areas.size());
                    final String areaCode = areas.get(areaIdx - 1).code().toString();
                    controller.addFlightControlOperator(username, password, firstName, lastName, email,
                            name, position, clearance, assessment, areaCode);
                    break;
                }
                case 3: {
                    // Select air control area from numbered list
                    final List<AirControlArea> areas = new ArrayList<>();
                    controller.allAirControlAreas().forEach(areas::add);
                    if (areas.isEmpty()) {
                        System.out.println("  [!] No Air Control Areas available. Please register some first.");
                        return false;
                    }
                    System.out.println("\nAvailable Air Control Areas:");
                    for (int i = 0; i < areas.size(); i++) {
                        final AirControlArea a = areas.get(i);
                        System.out.printf("  %d. %s - %s [lat %.2f to %.2f x lon %.2f to %.2f]%n",
                                i + 1, a.code(), a.name(),
                                a.minLat(), a.maxLat(), a.minLon(), a.maxLon());
                    }
                    int areaIdx;
                    do {
                        areaIdx = Console.readInteger("Select (1-" + areas.size() + ")");
                        if (areaIdx < 1 || areaIdx > areas.size()) {
                            System.out.println("  [!] Please enter a number between 1 and " + areas.size() + ".");
                        }
                    } while (areaIdx < 1 || areaIdx > areas.size());
                    final String areaCode = areas.get(areaIdx - 1).code().toString();
                    controller.addWeatherPerson(username, password, firstName, lastName, email,
                            name, position, clearance, assessment, areaCode);
                    break;
                }
                default:
                    System.out.println("Invalid type.");
                    return false;
            }
            System.out.println("  >> Collaborator added successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Error: username or email already in use.");
        } catch (final IllegalStateException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Add Collaborator (US061)";
    }
}
