package eapli.aisafe.flight.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

class FlightDesignatorTest {

    @Test
    void ensureValidDesignatorIsAccepted() {
        final var d = new FlightDesignator("TP1234");
        assertEquals("TP1234", d.toString());
    }

    @Test
    void ensureLowerCaseIsUppercased() {
        final var d = new FlightDesignator("tp5678");
        assertEquals("TP5678", d.toString());
    }

    @Test
    void ensureValueOfFactoryWorks() {
        final var d = FlightDesignator.valueOf("FR123");
        assertEquals("FR123", d.toString());
    }

    @Test
    void ensureNullDesignatorIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightDesignator(null));
    }

    @Test
    void ensureBlankDesignatorIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> new FlightDesignator("   "));
    }

    @Test
    void ensureInvalidFormatIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> new FlightDesignator("INVALID"));
    }

    @Test
    void ensureNumericDesignatorIsAccepted() {
        final var d = new FlightDesignator("12345");
        assertEquals("12345", d.toString());
    }

    @Test
    void ensureFormatWithTooManyDigitsIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> new FlightDesignator("TP12345"));
    }

    @Test
    void ensureFormatWithOptionalLetterIsAccepted() {
        final var d = new FlightDesignator("TP123A");
        assertEquals("TP123A", d.toString());
    }

    @Test
    void ensureOneDigitIsAccepted() {
        final var d = new FlightDesignator("TP1");
        assertEquals("TP1", d.toString());
    }

    @Test
    void ensureEqualsAndHashCode() {
        final var d1 = new FlightDesignator("TP1234");
        final var d2 = new FlightDesignator("tp1234");
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    void ensureNotEquals() {
        final var d1 = new FlightDesignator("TP1234");
        final var d2 = new FlightDesignator("TP5678");
        assertNotEquals(d1, d2);
    }

    @Test
    void ensureCompareToWorks() {
        final var d1 = new FlightDesignator("TP1000");
        final var d2 = new FlightDesignator("TP2000");
        assertTrue(d1.compareTo(d2) < 0);
        assertTrue(d2.compareTo(d1) > 0);
        assertEquals(0, d1.compareTo(new FlightDesignator("tp1000")));
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us074/flight_designator_test.csv", numLinesToSkip = 1)
    void ensureFlightDesignatorCsvInvariants(final String testCaseId, final String designator,
                                              final boolean expectedValid) {
        if (expectedValid) {
            assertDoesNotThrow(() -> new FlightDesignator(designator));
        } else {
            assertThrows(Exception.class, () -> new FlightDesignator(designator));
        }
    }
}
