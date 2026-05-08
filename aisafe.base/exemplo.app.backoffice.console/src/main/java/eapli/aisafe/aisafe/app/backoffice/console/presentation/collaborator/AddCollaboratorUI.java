package eapli.aisafe.app.backoffice.console.presentation.collaborator;

import eapli.aisafe.collaborator.application.AddCollaboratorController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * UI for US061 — Add Customer's Collaborator.
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
        final int type = Console.readInteger("Select type (1-3)");

        // Common fields
        final String username  = Console.readLine("Username (for system login)");
        final String password  = Console.readLine("Password");
        final String firstName = Console.readLine("First Name");
        final String lastName  = Console.readLine("Last Name");
        final String email     = Console.readLine("Email");
        final String name      = Console.readLine("Full Name / Display Name");
        final String position  = Console.readLine("Position / Job Title");
        final LocalDate clearance = LocalDate.parse(
                Console.readLine("Security Clearance Expiry Date (yyyy-MM-dd)"), DATE_FMT);
        final LocalDate assessment = LocalDate.parse(
                Console.readLine("Skills Assessment Date (yyyy-MM-dd)"), DATE_FMT);

        try {
            switch (type) {
                case 1: {
                    System.out.println("\nAvailable Companies:");
                    controller.allCompanies().forEach(c -> System.out.println("  " + c));
                    final String iata = Console.readLine("Company IATA Code");
                    controller.addATCCollaborator(username, password, firstName, lastName, email,
                            name, position, clearance, assessment, iata);
                    break;
                }
                case 2: {
                    System.out.println("\nAvailable Air Control Areas:");
                    controller.allAirControlAreas().forEach(a -> System.out.println("  " + a));
                    final String areaCode = Console.readLine("Area Code");
                    controller.addFlightControlOperator(username, password, firstName, lastName, email,
                            name, position, clearance, assessment, areaCode);
                    break;
                }
                case 3: {
                    System.out.println("\nAvailable Air Control Areas:");
                    controller.allAirControlAreas().forEach(a -> System.out.println("  " + a));
                    final String areaCode = Console.readLine("Area Code");
                    controller.addWeatherPerson(username, password, firstName, lastName, email,
                            name, position, clearance, assessment, areaCode);
                    break;
                }
                default:
                    System.out.println("Invalid type.");
                    return false;
            }
            System.out.println("Collaborator added successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Error: username or email already in use.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Add Collaborator (US061)";
    }
}
