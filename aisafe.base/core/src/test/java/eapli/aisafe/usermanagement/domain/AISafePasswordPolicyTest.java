package eapli.aisafe.usermanagement.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

class AISafePasswordPolicyTest {

    private final AISafePasswordPolicy passwordPolicy = new AISafePasswordPolicy();

    @Test
    void ensureValidPasswordMustHaveAtLeast8Characters() {
        assertFalse(passwordPolicy.isSatisfiedBy("short"));
        assertFalse(passwordPolicy.isSatisfiedBy("LongEnough"));   // no digit
        assertFalse(passwordPolicy.isSatisfiedBy("longenough123"));// no capital
        assertTrue(passwordPolicy.isSatisfiedBy("LongEnough123")); // satisfies all
    }

    @Test
    void ensureValidPasswordMustContainDigit() {
        assertFalse(passwordPolicy.isSatisfiedBy("NoDigitHereABC"));  // 14 chars, has capital, no digit
        assertTrue(passwordPolicy.isSatisfiedBy("WithDigit1AB"));      // has digit + capital + 12 chars
    }

    @Test
    void ensureValidPasswordMustContainCapitalLetter() {
        assertFalse(passwordPolicy.isSatisfiedBy("lowercase123"));  // has digit, no capital, >=8 chars
        assertTrue(passwordPolicy.isSatisfiedBy("Capital1234"));     // has capital, digit, >=8 chars
    }

    @Test
    void ensureValidPasswordMustHaveAllRequirements() {
        assertFalse(passwordPolicy.isSatisfiedBy("nocapitals12345"));   // 15 chars, has digit, no capital
        assertFalse(passwordPolicy.isSatisfiedBy("NoDigitABCDEF"));  // 13 chars, has capital, no digit
        assertFalse(passwordPolicy.isSatisfiedBy("Ab1"));            // too short (3 chars)
        assertTrue(passwordPolicy.isSatisfiedBy("ValidPASS123"));    // satisfies all
    }

    @Test
    void ensureEmptyOrNullPasswordIsInvalid() {
        assertFalse(passwordPolicy.isSatisfiedBy(""));
        assertFalse(passwordPolicy.isSatisfiedBy(null));
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us100/password_policy_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensurePasswordPolicyCsvInvariants(
            final String testCaseId,
            final String password,
            final boolean expectedValid
    ) {
        final boolean result = passwordPolicy.isSatisfiedBy(password);
        if (expectedValid) {
            assertTrue(result, "Expected valid password: " + password);
        } else {
            assertFalse(result, "Expected invalid password: " + password);
        }
    }
}