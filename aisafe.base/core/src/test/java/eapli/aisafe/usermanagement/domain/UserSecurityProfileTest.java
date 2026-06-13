package eapli.aisafe.usermanagement.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserSecurityProfileTest {

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us100/user_security_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureUserSecurityProfileInvariants(
            final String testCaseId,
            final String username,
            final String clearanceExpiryDateStr,
            final String phone,
            final boolean expectedValid
    ) {
        final LocalDate clearanceExpiryDate = LocalDate.parse(clearanceExpiryDateStr);

        if (expectedValid) {
            assertDoesNotThrow(() -> new UserSecurityProfile(username, clearanceExpiryDate, phone));
        } else {
            assertThrows(Exception.class, () -> new UserSecurityProfile(username, clearanceExpiryDate, phone));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us100/user_security_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureUserSecurityProfileEquals(
            final String testCaseId,
            final String username,
            final String clearanceExpiryDateStr,
            final String phone,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final LocalDate clearanceExpiryDate = LocalDate.parse(clearanceExpiryDateStr);
            final var profile1 = new UserSecurityProfile(username, clearanceExpiryDate, phone);
            final var profile2 = new UserSecurityProfile(username, clearanceExpiryDate, phone);
            assertEquals(profile1, profile2);
            assertEquals(profile1.hashCode(), profile2.hashCode());
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us100/user_security_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureUserSecurityProfileToString(
            final String testCaseId,
            final String username,
            final String clearanceExpiryDateStr,
            final String phone,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final LocalDate clearanceExpiryDate = LocalDate.parse(clearanceExpiryDateStr);
            final var profile = new UserSecurityProfile(username, clearanceExpiryDate, phone);
            assertNotNull(profile.toString());
            assertTrue(profile.toString().contains("UserSecurityProfile"));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us100/user_security_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureUserSecurityProfileGetters(
            final String testCaseId,
            final String username,
            final String clearanceExpiryDateStr,
            final String phone,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final LocalDate clearanceExpiryDate = LocalDate.parse(clearanceExpiryDateStr);
            final var profile = new UserSecurityProfile(username, clearanceExpiryDate, phone);
            assertNotNull(profile.identity());
            assertNotNull(profile.clearanceExpiryDate());
            assertNotNull(profile.phone());
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us100/user_security_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureUserSecurityProfileIsClearanceValid(
            final String testCaseId,
            final String username,
            final String clearanceExpiryDateStr,
            final String phone,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final LocalDate clearanceExpiryDate = LocalDate.parse(clearanceExpiryDateStr);
            final var profile = new UserSecurityProfile(username, clearanceExpiryDate, phone);
            assertTrue(profile.isClearanceValid());
        }
    }
}