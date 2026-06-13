package eapli.aisafe.simulation.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

class SafetyThresholdTest {

    @Test
    void ensureValidSafetyThresholdCanBeCreated() {
        final var st = new SafetyThreshold(10.0, "meters");
        assertEquals(10.0, st.value(), 0.001);
        assertEquals("meters", st.unit());
    }

    @Test
    void ensureValueMustBePositive() {
        assertThrows(Exception.class, () -> new SafetyThreshold(0.0, "meters"));
        assertThrows(Exception.class, () -> new SafetyThreshold(-1.0, "meters"));
    }

    @Test
    void ensureUnitMustNotBeBlank() {
        assertThrows(Exception.class, () -> new SafetyThreshold(10.0, ""));
        assertThrows(Exception.class, () -> new SafetyThreshold(10.0, "   "));
    }

    @Test
    void ensureUnitMustNotBeNull() {
        assertThrows(Exception.class, () -> new SafetyThreshold(10.0, null));
    }

    @Test
    void ensureUnitIsTrimmed() {
        final var st = new SafetyThreshold(5.0, "  meters  ");
        assertEquals("meters", st.unit());
    }

    @Test
    void ensureEqualsAndHashCode() {
        final var st1 = new SafetyThreshold(10.0, "meters");
        final var st2 = new SafetyThreshold(10.0, "meters");
        assertEquals(st1, st2);
        assertEquals(st1.hashCode(), st2.hashCode());
    }

    @Test
    void ensureNotEqualsDifferentValue() {
        final var st1 = new SafetyThreshold(10.0, "meters");
        final var st2 = new SafetyThreshold(20.0, "meters");
        assertNotEquals(st1, st2);
    }

    @Test
    void ensureNotEqualsDifferentUnit() {
        final var st1 = new SafetyThreshold(10.0, "meters");
        final var st2 = new SafetyThreshold(10.0, "seconds");
        assertNotEquals(st1, st2);
    }

    @Test
    void ensureToStringContainsValueAndUnit() {
        final var st = new SafetyThreshold(15.0, "meters");
        final var s = st.toString();
        assertTrue(s.contains("15.0") || s.contains("15"));
        assertTrue(s.contains("meters"));
    }

    @ParameterizedTest(name = "{0}: value={1} unit={2} expectedValid={3}")
    @CsvFileSource(resources = "/us070/safety_threshold_test.csv", numLinesToSkip = 1)
    void ensureSafetyThresholdCsvInvariants(
            final String testCaseId, final double value,
            final String unit, final boolean expectedValid) {
        if (expectedValid) {
            assertDoesNotThrow(() -> new SafetyThreshold(value, unit));
        } else {
            assertThrows(Exception.class, () -> new SafetyThreshold(value, unit));
        }
    }
}
