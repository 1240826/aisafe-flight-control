/*
 * Copyright (c) 2013-2024 the original author or authors.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package eapli.aisafe.ui.authz;

import eapli.aisafe.usermanagement.application.AddUserController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * US031 — Add User UI.
 * Collects user data including multi-role selection and security clearance expiry (AC 031.7).
 */
@SuppressWarnings("squid:S106")
public class AddUserUI extends AbstractUI {

    private final AddUserController theController = new AddUserController();

    @Override
    protected boolean doShow() {

        // --- Username: non-blank ---
        String username;
        do {
            username = Console.readLine("Username").trim();
            if (username.isBlank()) System.out.println("  [!] Username cannot be blank. Please try again.");
        } while (username.isBlank());

        // --- Password: min 8 chars, 1 digit, 1 capital ---
        String password;
        do {
            System.out.println("  Password requirements: min. 8 characters, at least 1 digit and 1 capital letter.");
            password = Console.readLine("Password");
            final boolean ok = password.length() >= 8
                    && password.chars().anyMatch(Character::isDigit)
                    && password.chars().anyMatch(Character::isUpperCase);
            if (!ok) {
                System.out.println("  [!] Password does not meet requirements. Please try again.");
                password = null;
            }
        } while (password == null);

        // --- First / Last name ---
        String firstName;
        do {
            firstName = Console.readLine("First Name").trim();
            if (firstName.isBlank()) System.out.println("  [!] First Name cannot be blank.");
        } while (firstName.isBlank());

        String lastName;
        do {
            lastName = Console.readLine("Last Name").trim();
            if (lastName.isBlank()) System.out.println("  [!] Last Name cannot be blank.");
        } while (lastName.isBlank());

        // --- E-mail: must match local@domain.tld (basic sanity check) ---
        String email = null;
        while (email == null) {
            final String emailInput = Console.readLine("E-Mail (e.g. user@example.com)").trim();
            if (!emailInput.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
                System.out.println("  [!] Please enter a valid e-mail address (e.g. user@example.com).");
            } else {
                email = emailInput;
            }
        }

        // --- Phone Number (spec §3.1.1: users have name and phone number) ---
        String phone;
        do {
            phone = Console.readLine("Phone Number (e.g. +351912345678)").trim();
            if (phone.isBlank()) System.out.println("  [!] Phone number cannot be blank.");
        } while (phone.isBlank());

        // --- Security Clearance Expiry Date (AC 031.7) ---
        LocalDate clearanceExpiryDate = null;
        while (clearanceExpiryDate == null) {
            try {
                clearanceExpiryDate = LocalDate.parse(
                        Console.readLine("Security Clearance Expiry Date (YYYY-MM-DD)").trim());
                if (clearanceExpiryDate.isBefore(LocalDate.now())) {
                    System.out.println("  [!] Expiry date must be today or in the future.");
                    clearanceExpiryDate = null;
                }
            } catch (final DateTimeParseException e) {
                System.out.println("  [!] Invalid date format. Use YYYY-MM-DD (e.g. 2027-12-31).");
            }
        }

        // --- Role multi-selection ---
        final Set<Role> roleTypes = selectRoles();
        if (roleTypes == null) return false; // user aborted (no roles available)

        // --- Persist ---
        try {
            this.theController.addUser(username, password, firstName, lastName, email,
                    roleTypes, clearanceExpiryDate, phone);
            System.out.println("  >> User registered successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("  [!] That username or e-mail is already in use.");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [!] " + e.getMessage());
        }

        return false;
    }

    /**
     * Interactive multi-role selection loop.
     * Keeps showing the available roles until the user chooses "Done".
     * Requires at least one role to be selected.
     *
     * @return the selected roles, or {@code null} if no roles are available
     */
    private Set<Role> selectRoles() {
        final List<Role> available = new ArrayList<>(Arrays.asList(theController.getRoleTypes()));
        if (available.isEmpty()) {
            System.out.println("  [!] No roles available in the system.");
            return null;
        }

        final Set<Role> selected = new LinkedHashSet<>();

        while (true) {
            System.out.println("\nAvailable roles (select one or more, then choose 0 when done):");
            for (int i = 0; i < available.size(); i++) {
                final Role r = available.get(i);
                final String tick = selected.contains(r) ? " [*]" : "";
                System.out.printf("  %d. %s%s%n", i + 1, r, tick);
            }
            if (!selected.isEmpty()) {
                System.out.println("  Selected: " + selected);
            }
            System.out.println("  0. Done (confirm selection)");

            final int choice = Console.readInteger("Please choose an option");

            if (choice == 0) {
                if (selected.isEmpty()) {
                    System.out.println("  [!] At least one role must be assigned. Please select a role.");
                } else {
                    break; // valid — exit loop
                }
            } else if (choice >= 1 && choice <= available.size()) {
                final Role role = available.get(choice - 1);
                if (selected.add(role)) {
                    System.out.println("  [+] Role '" + role + "' added.");
                } else {
                    selected.remove(role);
                    System.out.println("  [-] Role '" + role + "' removed.");
                }
            } else {
                System.out.println("  [!] Invalid option. Please enter a number between 0 and " + available.size() + ".");
            }
        }

        return selected;
    }

    @Override
    public String headline() {
        return "Add User";
    }
}
