package eapli.aisafe.flightplan.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationResultTest {

    @Test
    void passedResult_isPassed() {
        final var result = ValidationResult.passed();
        assertTrue(result.isPassed());
        assertTrue(result.reasons().isEmpty());
    }

    @Test
    void failedResult_isNotPassed() {
        final var result = ValidationResult.failed("Invalid altitude");
        assertFalse(result.isPassed());
        assertEquals(1, result.reasons().size());
        assertTrue(result.reasons().get(0).contains("Invalid altitude"));
    }

    @Test
    void failedResult_withMultipleReasons() {
        final var reasons = List.of("Invalid altitude", "Exceeds max weight");
        final var result = ValidationResult.failed(reasons);
        assertFalse(result.isPassed());
        assertEquals(2, result.reasons().size());
    }

    @Test
    void failedResult_nullSingleReason_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidationResult.failed((String) null));
    }

    @Test
    void failedResult_blankReason_throws() {
        assertThrows(IllegalStateException.class,
                () -> ValidationResult.failed("   "));
    }

    @Test
    void failedResult_emptyList_throws() {
        assertThrows(IllegalStateException.class,
                () -> ValidationResult.failed(List.of()));
    }

    @Test
    void passedResult_toString() {
        assertEquals("PASSED", ValidationResult.passed().toString());
    }

    @Test
    void failedResult_toString() {
        final var result = ValidationResult.failed("Low fuel");
        assertTrue(result.toString().contains("FAILED"));
        assertTrue(result.toString().contains("Low fuel"));
    }

    @Test
    void passedAndFailedAreNotEqual() {
        assertNotEquals(ValidationResult.passed(),
                ValidationResult.failed("Some reason"));
    }
}
