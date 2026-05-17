package eapli.aisafe.collaborator.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SecurityClearance and SkillsAssessment value objects.
 * Covers US061 and US063 invariants.
 */
class SecurityClearanceAndSkillsAssessmentTest {

    // ── SecurityClearance ─────────────────────────────────────────────────────

    @Test
    void ensureValidSecurityClearanceCanBeCreated() {
        assertNotNull(new SecurityClearance(LocalDate.now().plusYears(1)));
    }

    @Test
    void ensureExpiryDateIsPreserved() {
        final var expiry = LocalDate.now().plusYears(2);
        assertEquals(expiry, new SecurityClearance(expiry).expiryDate());
    }

    @Test
    void ensureTodayExpiryIsAccepted() {
        assertDoesNotThrow(() -> new SecurityClearance(LocalDate.now()));
    }

    @Test
    void ensurePastExpiryDateIsRejected() {
        assertThrows(Exception.class,
                () -> new SecurityClearance(LocalDate.now().minusDays(1)));
    }

    @Test
    void ensureNullExpiryDateIsRejected() {
        assertThrows(Exception.class, () -> new SecurityClearance(null));
    }

    @Test
    void ensureIsValidReturnsTrueForFutureExpiry() {
        assertTrue(new SecurityClearance(LocalDate.now().plusYears(1)).isValid());
    }

    @Test
    void ensureIsValidReturnsTrueForTodayExpiry() {
        assertTrue(new SecurityClearance(LocalDate.now()).isValid());
    }

    @Test
    void ensureTwoEqualSecurityClearancesAreEqual() {
        final var expiry = LocalDate.now().plusYears(1);
        assertEquals(new SecurityClearance(expiry), new SecurityClearance(expiry));
    }

    @Test
    void ensureDifferentExpiryDatesAreNotEqual() {
        assertNotEquals(
                new SecurityClearance(LocalDate.now().plusYears(1)),
                new SecurityClearance(LocalDate.now().plusYears(2)));
    }

    // ── SkillsAssessment ──────────────────────────────────────────────────────

    @Test
    void ensureValidSkillsAssessmentCanBeCreated() {
        assertNotNull(new SkillsAssessment(LocalDate.now().minusMonths(6)));
    }

    @Test
    void ensureAssessmentDateIsPreserved() {
        final var date = LocalDate.now().minusMonths(3);
        assertEquals(date, new SkillsAssessment(date).assessmentDate());
    }

    @Test
    void ensureTodayAssessmentIsAccepted() {
        assertDoesNotThrow(() -> new SkillsAssessment(LocalDate.now()));
    }

    @Test
    void ensureFutureAssessmentDateIsRejected() {
        assertThrows(Exception.class,
                () -> new SkillsAssessment(LocalDate.now().plusDays(1)));
    }

    @Test
    void ensureNullAssessmentDateIsRejected() {
        assertThrows(Exception.class, () -> new SkillsAssessment(null));
    }

    @Test
    void ensureIsExpiredByRegulationsReturnsFalseWhenRecent() {
        assertFalse(new SkillsAssessment(LocalDate.now().minusYears(1)).isExpiredByRegulations());
    }

    @Test
    void ensureIsExpiredByRegulationsReturnsTrueWhenOld() {
        assertTrue(new SkillsAssessment(LocalDate.now().minusYears(6)).isExpiredByRegulations());
    }

    @Test
    void ensureNextDueDateIsCorrect() {
        final var date = LocalDate.of(2020, 1, 1);
        assertEquals(LocalDate.of(2025, 1, 1), new SkillsAssessment(date).nextDueDate());
    }

    @Test
    void ensureTwoEqualSkillsAssessmentsAreEqual() {
        final var date = LocalDate.now().minusMonths(6);
        assertEquals(new SkillsAssessment(date), new SkillsAssessment(date));
    }

    @Test
    void ensureDifferentAssessmentDatesAreNotEqual() {
        assertNotEquals(
                new SkillsAssessment(LocalDate.now().minusMonths(6)),
                new SkillsAssessment(LocalDate.now().minusMonths(3)));
    }
    @Test
    void ensureSecurityClearanceToStringIsNotBlank() {
        assertFalse(new SecurityClearance(LocalDate.now().plusYears(1)).toString().isBlank());
    }

    @Test
    void ensureSkillsAssessmentToStringIsNotBlank() {
        assertFalse(new SkillsAssessment(LocalDate.now().minusMonths(1)).toString().isBlank());
    }

    @Test
    void ensureSecurityClearanceHashCodeIsConsistent() {
        final var expiry = LocalDate.now().plusYears(1);
        assertEquals(new SecurityClearance(expiry).hashCode(),
                new SecurityClearance(expiry).hashCode());
    }

    @Test
    void ensureSkillsAssessmentHashCodeIsConsistent() {
        final var date = LocalDate.now().minusMonths(1);
        assertEquals(new SkillsAssessment(date).hashCode(),
                new SkillsAssessment(date).hashCode());
    }
    @Test
    void ensureSecurityClearanceNotEqualToNull() {
        assertNotEquals(new SecurityClearance(LocalDate.now().plusYears(1)), null);
    }

    @Test
    void ensureSecurityClearanceNotEqualToDifferentType() {
        assertNotEquals(new SecurityClearance(LocalDate.now().plusYears(1)), "not a clearance");
    }
    @Test
    void ensureSkillsAssessmentNotEqualToNull() {
        assertNotEquals(new SkillsAssessment(LocalDate.now().minusMonths(1)), null);
    }

    @Test
    void ensureSkillsAssessmentNotEqualToDifferentType() {
        assertNotEquals(new SkillsAssessment(LocalDate.now().minusMonths(1)), "not an assessment");
    }
}