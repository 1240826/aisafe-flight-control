package eapli.aisafe.manufacturer.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Manufacturer aggregate root.
 * Covers US055 invariants: name non-blank, country non-blank, identity.
 */
class ManufacturerTest {

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureValidManufacturerCanBeCreatedWithStrings() {
        final var m = new Manufacturer("Airbus", "France");
        assertEquals("Airbus", m.name().toString());
        assertEquals("France", m.country());
    }

    @Test
    void ensureValidManufacturerCanBeCreatedWithVO() {
        final var name = new ManufacturerName("Boeing");
        final var m = new Manufacturer(name, "United States");
        assertEquals(name, m.identity());
        assertEquals("United States", m.country());
    }

    @Test
    void ensureIdentityReturnsManufacturerName() {
        final var name = new ManufacturerName("Embraer");
        final var m = new Manufacturer(name, "Brazil");
        assertEquals(name, m.identity());
    }

    @Test
    void ensureNameWithLeadingTrailingWhitespaceIsTrimmed() {
        // ManufacturerName trims leading/trailing whitespace in its constructor
        final var m = new Manufacturer("  Boeing  ", "USA");
        assertEquals("Boeing", m.name().toString(),
                "Manufacturer name must be trimmed of leading/trailing whitespace");
    }

    // ── Invariant violations ──────────────────────────────────────────────────

    @Test
    void ensureBlankNameIsRejected() {
        assertThrows(Exception.class, () -> new Manufacturer("", "France"));
    }

    @Test
    void ensureWhitespaceOnlyNameIsRejected() {
        assertThrows(Exception.class, () -> new Manufacturer("   ", "USA"));
    }

    @Test
    void ensureBlankCountryIsRejected() {
        assertThrows(Exception.class, () -> new Manufacturer("Airbus", ""));
    }

    @Test
    void ensureWhitespaceOnlyCountryIsRejected() {
        assertThrows(Exception.class, () -> new Manufacturer("Boeing", "   "));
    }

    @Test
    void ensureNullCountryIsRejected() {
        assertThrows(Exception.class, () -> new Manufacturer("Airbus", null));
    }

    @Test
    void ensureNullManufacturerNameVOIsRejected() {
        assertThrows(Exception.class, () -> new Manufacturer((ManufacturerName) null, "France"));
    }

    // ── ManufacturerName VO ───────────────────────────────────────────────────

    @Test
    void ensureBlankManufacturerNameVOIsRejected() {
        assertThrows(Exception.class, () -> new ManufacturerName(""));
    }

    @Test
    void ensureNullManufacturerNameStringIsRejected() {
        assertThrows(Exception.class, () -> new ManufacturerName(null));
    }

    // ── Equality ──────────────────────────────────────────────────────────────

    @Test
    void ensureManufacturersWithSameNameAreEqual() {
        final var m1 = new Manufacturer("Boeing", "USA");
        final var m2 = new Manufacturer("Boeing", "United States");
        // Identity is ManufacturerName — same name implies same entity
        assertEquals(m1.identity(), m2.identity());
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/manufacturer_test.csv", numLinesToSkip = 1)
    void ensureManufacturerCsvInvariants(final String testCaseId, final String name,
                                          final String country, final boolean expectedValid) {
        if (expectedValid) {
            assertDoesNotThrow(() -> new Manufacturer(name, country));
        } else {
            assertThrows(Exception.class, () -> new Manufacturer(name, country));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/manufacturer_test.csv", numLinesToSkip = 1)
    void ensureManufacturerEquals(final String testCaseId, final String name,
                                   final String country, final boolean expectedValid) {
        if (expectedValid) {
            final var m1 = new Manufacturer(name, country);
            final var m2 = new Manufacturer(name, country);
            assertEquals(m1, m2);
            assertEquals(m1.hashCode(), m2.hashCode());
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/manufacturer_test.csv", numLinesToSkip = 1)
    void ensureManufacturerToString(final String testCaseId, final String name,
                                     final String country, final boolean expectedValid) {
        if (expectedValid) {
            final var m = new Manufacturer(name, country);
            assertNotNull(m.toString());
            assertTrue(m.toString().contains(name.trim()));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/manufacturer_test.csv", numLinesToSkip = 1)
    void ensureManufacturerGetters(final String testCaseId, final String name,
                                     final String country, final boolean expectedValid) {
        if (expectedValid) {
            final var m = new Manufacturer(name, country);
            assertNotNull(m.identity());
            assertNotNull(m.name());
            assertNotNull(m.country());
        }
    }
}
