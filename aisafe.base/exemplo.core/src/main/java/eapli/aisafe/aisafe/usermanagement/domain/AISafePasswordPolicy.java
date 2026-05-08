package eapli.aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.PasswordPolicy;
import eapli.framework.strings.util.StringPredicates;

/**
 * AISafe password policy: min 8 chars, at least 1 digit and 1 capital letter.
 * US030 — auth infrastructure.
 */
public class AISafePasswordPolicy implements PasswordPolicy {

    @Override
    public boolean isSatisfiedBy(final String rawPassword) {
        if (StringPredicates.isNullOrEmpty(rawPassword)) {
            return false;
        }
        if (rawPassword.length() < 8) {
            return false;
        }
        if (!StringPredicates.containsDigit(rawPassword)) {
            return false;
        }
        return StringPredicates.containsCapital(rawPassword);
    }
}
