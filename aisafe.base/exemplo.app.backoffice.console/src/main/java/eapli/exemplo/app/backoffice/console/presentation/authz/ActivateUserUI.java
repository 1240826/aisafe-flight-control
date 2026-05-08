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
package eapli.exemplo.app.backoffice.console.presentation.authz;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.exemplo.usermanagement.application.DeactivateUserController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * US032 — Enable (re-activate) a disabled user account (AC 032.3).
 * Symmetric to DeactivateUserUI.
 */
@SuppressWarnings("squid:S106")
public class ActivateUserUI extends AbstractUI {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivateUserUI.class);

    private final DeactivateUserController theController = new DeactivateUserController();

    @Override
    protected boolean doShow() {
        final List<SystemUser> list = new ArrayList<>();
        final Iterable<SystemUser> iterable = this.theController.deactivatedUsers();
        if (!iterable.iterator().hasNext()) {
            System.out.println("There are no disabled users to re-enable.");
        } else {
            int cont = 1;
            System.out.println("SELECT User to re-enable\n");
            System.out.printf("%-6s%-10s%-30s%-30s%n", "Nº:", "Username", "Firstname", "Lastname");
            for (final SystemUser user : iterable) {
                list.add(user);
                System.out.printf("%-6d%-10s%-30s%-30s%n", cont, user.username(),
                        user.name().firstName(), user.name().lastName());
                cont++;
            }
            final int option = Console.readInteger("Enter user nº to re-enable or 0 to cancel ");
            if (option == 0) {
                System.out.println("No user selected.");
            } else {
                try {
                    this.theController.activateUser(list.get(option - 1));
                    System.out.println("User re-enabled successfully.");
                } catch (final IntegrityViolationException | ConcurrencyException ex) {
                    LOGGER.error("Error performing the operation", ex);
                    System.out.println("Unexpected error. Please try again.");
                }
            }
        }
        return true;
    }

    @Override
    public String headline() {
        return "Enable User";
    }
}
