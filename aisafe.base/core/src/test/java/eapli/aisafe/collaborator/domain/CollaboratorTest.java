package eapli.aisafe.collaborator.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Collaborator hierarchy.
 * Covers US061 (creation), US062 (active state), US063 (edit), US064 (disable).
 */
class CollaboratorTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static SystemUser dummySystemUser() {
        final SystemUserBuilder b = new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder());
        return b.with("testuser", "Password1", "Test", "User", "test@aisafe.pt")
                .withRoles(Role.valueOf("ADMIN"))
                .build();
    }

    private static SecurityClearance validClearance() {
        return new SecurityClearance(LocalDate.now().plusYears(1));
    }

    private static SkillsAssessment validAssessment() {
        return new SkillsAssessment(LocalDate.now().minusMonths(1));
    }

    private static ATCCollaborator validATCCollaborator() {
        return new ATCCollaborator(
                dummySystemUser(),
                "Alice Smith",
                "ATC Officer",
                validClearance(),
                validAssessment(),
                new CompanyIATA("TP")
        );
    }

    private static FlightControlOperator validFCO() {
        return new FlightControlOperator(
                dummySystemUser(),
                "Bob Jones",
                "FCO Senior",
                validClearance(),
                validAssessment(),
                new AreaCode("LPPC")
        );
    }

    private static WeatherPerson validWeatherPerson() {
        return new WeatherPerson(
                dummySystemUser(),
                "Carol White",
                "Meteorologist",
                validClearance(),
                validAssessment(),
                new AreaCode("LPPC")
        );
    }

    // ── Creation — ATC Collaborator (US061) ───────────────────────────────────

    @Test
    void ensureATCCollaboratorIsActiveOnCreation() {
        final var c = validATCCollaborator();
        assertTrue(c.isActive());
    }

    @Test
    void ensureATCCollaboratorNameIsPreserved() {
        final var c = validATCCollaborator();
        assertEquals("Alice Smith", c.name());
    }

    @Test
    void ensureATCCollaboratorPositionIsPreserved() {
        final var c = validATCCollaborator();
        assertEquals("ATC Officer", c.position());
    }

    @Test
    void ensureATCCollaboratorHasCompanyId() {
        final var c = validATCCollaborator();
        assertEquals(new CompanyIATA("TP"), c.companyId());
        assertNull(c.areaCode(), "ATCCollaborator must not have an AreaCode");
    }

    // ── Creation — FlightControlOperator (US061) ──────────────────────────────

    @Test
    void ensureFCOIsActiveOnCreation() {
        final var c = validFCO();
        assertTrue(c.isActive());
    }

    @Test
    void ensureFCOHasAreaCode() {
        final var c = validFCO();
        assertEquals(new AreaCode("LPPC"), c.areaCode());
        assertNull(c.companyId(), "FCO must not have a CompanyId");
    }

    // ── Creation — WeatherPerson (US061) ──────────────────────────────────────

    @Test
    void ensureWeatherPersonIsActiveOnCreation() {
        final var c = validWeatherPerson();
        assertTrue(c.isActive());
    }

    @Test
    void ensureWeatherPersonHasAreaCode() {
        final var c = validWeatherPerson();
        assertEquals(new AreaCode("LPPC"), c.areaCode());
        assertNull(c.companyId(), "WeatherPerson must not have a CompanyId");
    }

    // ── Disable (US064) ───────────────────────────────────────────────────────

    @Test
    void ensureActiveCollaboratorCanBeDisabled() {
        final var c = validATCCollaborator();
        c.disable();
        assertFalse(c.isActive());
    }

    @Test
    void ensureDisabledCollaboratorCannotBeDisabledAgain() {
        final var c = validATCCollaborator();
        c.disable();
        assertThrows(IllegalStateException.class, c::disable,
                "Disabling an already disabled collaborator must throw");
    }

    // ── Edit — name and position (US063) ──────────────────────────────────────

    @Test
    void ensureNameCanBeUpdated() {
        final var c = validATCCollaborator();
        c.updateName("Alice Johnson");
        assertEquals("Alice Johnson", c.name());
    }

    @Test
    void ensureBlankNameUpdateIsRejected() {
        final var c = validATCCollaborator();
        assertThrows(Exception.class, () -> c.updateName(""),
                "Blank name update must be rejected");
    }

    @Test
    void ensureWhitespaceOnlyNameUpdateIsRejected() {
        final var c = validATCCollaborator();
        assertThrows(Exception.class, () -> c.updateName("   "));
    }

    @Test
    void ensurePositionCanBeUpdated() {
        final var c = validATCCollaborator();
        c.updatePosition("Senior ATC Officer");
        assertEquals("Senior ATC Officer", c.position());
    }

    @Test
    void ensureBlankPositionUpdateIsRejected() {
        final var c = validATCCollaborator();
        assertThrows(Exception.class, () -> c.updatePosition(""));
    }

    // ── Edit — security clearance (US063) ─────────────────────────────────────

    @Test
    void ensureSecurityClearanceCanBeRenewed() {
        final var c = validATCCollaborator();
        final var newClearance = new SecurityClearance(LocalDate.now().plusYears(3));
        c.renewSecurityClearance(newClearance);
        assertEquals(LocalDate.now().plusYears(3), c.securityClearance().expiryDate());
    }

    @Test
    void ensureRenewingWithPastClearanceDateIsRejected() {
        // SecurityClearance invariant: expiry must not be in the past
        assertThrows(Exception.class,
                () -> new SecurityClearance(LocalDate.now().minusDays(1)),
                "Past expiry date must be rejected");
    }

    // ── Edit — skills assessment (US063) ──────────────────────────────────────

    @Test
    void ensureSkillsAssessmentCanBeUpdated() {
        final var c = validATCCollaborator();
        final var newAssessment = new SkillsAssessment(LocalDate.now());
        c.updateSkillsAssessment(newAssessment);
        assertEquals(LocalDate.now(), c.skillsAssessment().assessmentDate());
    }

    @Test
    void ensureFutureSkillsAssessmentDateIsRejected() {
        // SkillsAssessment invariant: date must not be in the future
        assertThrows(Exception.class,
                () -> new SkillsAssessment(LocalDate.now().plusDays(1)),
                "Future assessment date must be rejected");
    }

    // ── Invariant violations — constructor ────────────────────────────────────

    @Test
    void ensureNullSystemUserIsRejected() {
        assertThrows(Exception.class, () -> new ATCCollaborator(
                null, "Alice Smith", "ATC Officer",
                validClearance(), validAssessment(), new CompanyIATA("TP")));
    }

    @Test
    void ensureBlankCollaboratorNameIsRejected() {
        assertThrows(Exception.class, () -> new ATCCollaborator(
                dummySystemUser(), "", "ATC Officer",
                validClearance(), validAssessment(), new CompanyIATA("TP")));
    }

    @Test
    void ensureBlankPositionIsRejected() {
        assertThrows(Exception.class, () -> new ATCCollaborator(
                dummySystemUser(), "Alice Smith", "",
                validClearance(), validAssessment(), new CompanyIATA("TP")));
    }

    @Test
    void ensureNullCompanyIdForATCIsRejected() {
        assertThrows(Exception.class, () -> new ATCCollaborator(
                dummySystemUser(), "Alice Smith", "ATC Officer",
                validClearance(), validAssessment(), null));
    }

    @Test
    void ensureNullAreaCodeForFCOIsRejected() {
        assertThrows(Exception.class, () -> new FlightControlOperator(
                dummySystemUser(), "Bob Jones", "FCO Senior",
                validClearance(), validAssessment(), null));
    }

    // ── Edit — phone (US063) ──────────────────────────────────────────────────

    @Test
    void ensurePhoneIsNullByDefault() {
        final var c = validATCCollaborator();
        assertNull(c.phone(), "Phone must be null when not set");
    }

    @Test
    void ensurePhoneCanBeSet() {
        final var c = validATCCollaborator();
        c.updatePhone("+351912345678");
        assertEquals("+351912345678", c.phone());
    }

    @Test
    void ensurePhoneIsTrimmedOnSet() {
        final var c = validATCCollaborator();
        c.updatePhone("  +351912345678  ");
        assertEquals("+351912345678", c.phone());
    }

    @Test
    void ensureBlankPhoneClearsPhone() {
        final var c = validATCCollaborator();
        c.updatePhone("+351912345678");
        c.updatePhone("");
        assertNull(c.phone(), "Blank phone must clear the value");
    }

    @Test
    void ensureNullPhoneClearsPhone() {
        final var c = validATCCollaborator();
        c.updatePhone("+351912345678");
        c.updatePhone(null);
        assertNull(c.phone(), "Null phone must clear the value");
    }
}
