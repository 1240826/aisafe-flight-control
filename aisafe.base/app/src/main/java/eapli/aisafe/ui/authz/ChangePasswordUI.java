/*
 * Copyright (c) 2013-2024 the original author or authors.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package eapli.aisafe.ui.authz;

import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.application.AuthenticationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI for user login action. Created by nuno on 21/03/16.
 */
@SuppressWarnings("squid:S106")
public class ChangePasswordUI extends AbstractUI {

    private final AuthenticationService authenticationService = AuthzRegistry.authenticationService();

    @Override
    protected boolean doShow() {
        // oldPassword — retry if blank
        String oldPassword;
        do {
            oldPassword = Console.readLine("Old Password:");
            if (oldPassword.isBlank()) {
                System.out.println("  [!] Old password cannot be blank. Please try again.");
            }
        } while (oldPassword.isBlank());

        // newPassword — retry with policy check (8+ chars, 1 digit, 1 capital)
        String newPassword;
        do {
            System.out.println("  Password requirements: min. 8 characters, at least 1 digit and 1 capital letter.");
            newPassword = Console.readLine("New Password:");
            if (newPassword.length() < 8
                    || !newPassword.chars().anyMatch(Character::isDigit)
                    || !newPassword.chars().anyMatch(Character::isUpperCase)) {
                System.out.println("  [!] Password does not meet requirements. Please try again.");
                newPassword = null;
            }
        } while (newPassword == null);

        try {
            if (this.authenticationService.changePassword(oldPassword, newPassword)) {
                System.out.println("  >> Password successfully changed.");
                return true;
            } else {
                System.out.println("  [!] Invalid authentication.");
                return false;
            }
        } catch (ConcurrencyException | IntegrityViolationException e) {
            System.out.println("An error has occurred> " + e.getLocalizedMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Change Password";
    }
}
