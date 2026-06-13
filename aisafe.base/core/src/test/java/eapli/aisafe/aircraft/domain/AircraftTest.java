package eapli.aisafe.aircraft.domain;

import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Aircraft aggregate root.
 * Covers US070 (creation), US071 (decommission), US072 (capacity).
 */
class AircraftTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static CabinConfiguration cabin170() {
        return new CabinConfiguration(List.of(
                new SeatClass("Economy", 150),
                new SeatClass("Business", 20)
        ));
    }

    /** Registration date 5 years ago (clearly in the past, non-zero age). */
    private static final LocalDate REG_DATE = LocalDate.now().minusYears(5);

    private static Aircraft validAircraft() {
        return new Aircraft(
                new RegistrationNumber("CS-TFG", "Portugal"),
                new AircraftModelCode("B738"),
                new CompanyIATA("TP"),
                2,
                cabin170(),
                REG_DATE
        );
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureNewAircraftIsActiveOnCreation() {
        // US070.7: aircraft is initially ACTIVE
        final var a = validAircraft();
        assertEquals(OperationalStatus.ACTIVE, a.operationalStatus());
        assertTrue(a.isActive());
    }

    @Test
    void ensureRegistrationNumberIsPreserved() {
        final var a = validAircraft();
        assertEquals(new RegistrationNumber("CS-TFG", "Portugal"), a.identity());
    }

    @Test
    void ensureFlightCrewMembersIsPreserved() {
        final var a = validAircraft();
        assertEquals(2, a.numberOfFlightCrewMembers());
    }

    @Test
    void ensureTotalCapacityIsCorrect() {
        // US072: 150 economy + 20 business = 170
        final var a = validAircraft();
        assertEquals(170, a.totalCapacity());
    }

    @Test
    void ensureAircraftWithSingleClassHasCorrectCapacity() {
        final var cabin = new CabinConfiguration(List.of(new SeatClass("Economy", 200)));
        final var a = new Aircraft(
                new RegistrationNumber("CS-TST", "Portugal"),
                new AircraftModelCode("A320"),
                new CompanyIATA("FR"),
                2, cabin, LocalDate.now().minusYears(3));
        assertEquals(200, a.totalCapacity());
    }

    // ── Registration date / age (US072d) ────────────────────────────────────

    @Test
    void ensureRegistrationDateIsPreserved() {
        final var a = validAircraft();
        assertEquals(REG_DATE, a.registrationDate());
    }

    @Test
    void ensureAgeInYearsIsCorrect() {
        // REG_DATE = 5 years ago → ageInYears should be 5
        final var a = validAircraft();
        assertEquals(5, a.ageInYears());
    }

    @Test
    void ensureFuturRegistrationDateIsRejected() {
        assertThrows(Exception.class, () -> new Aircraft(
                new RegistrationNumber("CS-TFG", "Portugal"),
                new AircraftModelCode("B738"),
                new CompanyIATA("TP"),
                2, cabin170(), LocalDate.now().plusDays(1)));
    }

    // ── Decommission (US071) ──────────────────────────────────────────────────

    @Test
    void ensureActiveAircraftCanBeDecommissioned() {
        final var a = validAircraft();
        a.decommission();
        assertEquals(OperationalStatus.DECOMMISSIONED, a.operationalStatus());
        assertFalse(a.isActive());
    }

    @Test
    void ensureDecommissionedAircraftCannotBeDecommissionedAgain() {
        final var a = validAircraft();
        a.decommission();
        assertThrows(IllegalStateException.class, a::decommission,
                "Decommissioning an already decommissioned aircraft must throw");
    }

    // ── Invariant violations ──────────────────────────────────────────────────

    @Test
    void ensureZeroFlightCrewMembersIsRejected() {
        // US070.8: crew count must be positive
        assertThrows(Exception.class, () -> new Aircraft(
                new RegistrationNumber("CS-TFG", "Portugal"),
                new AircraftModelCode("B738"),
                new CompanyIATA("TP"),
                0, cabin170(), REG_DATE));
    }

    @Test
    void ensureNegativeFlightCrewMembersIsRejected() {
        assertThrows(Exception.class, () -> new Aircraft(
                new RegistrationNumber("CS-TFG", "Portugal"),
                new AircraftModelCode("B738"),
                new CompanyIATA("TP"),
                -1, cabin170(), REG_DATE));
    }

    @Test
    void ensureNullRegistrationNumberIsRejected() {
        assertThrows(Exception.class, () -> new Aircraft(
                null,
                new AircraftModelCode("B738"),
                new CompanyIATA("TP"),
                2, cabin170(), REG_DATE));
    }

    @Test
    void ensureNullAircraftModelCodeIsRejected() {
        assertThrows(Exception.class, () -> new Aircraft(
                new RegistrationNumber("CS-TFG", "Portugal"),
                null,
                new CompanyIATA("TP"),
                2, cabin170(), REG_DATE));
    }

    @Test
    void ensureNullCompanyIdIsRejected() {
        assertThrows(Exception.class, () -> new Aircraft(
                new RegistrationNumber("CS-TFG", "Portugal"),
                new AircraftModelCode("B738"),
                null,
                2, cabin170(), REG_DATE));
    }

    @Test
    void ensureNullCabinConfigurationIsRejected() {
        assertThrows(Exception.class, () -> new Aircraft(
                new RegistrationNumber("CS-TFG", "Portugal"),
                new AircraftModelCode("B738"),
                new CompanyIATA("TP"),
                2, null, REG_DATE));
    }

    // ── RegistrationNumber VO ─────────────────────────────────────────────────

    @Test
    void ensureBlankRegistrationNumberIsRejected() {
        assertThrows(Exception.class, () -> new RegistrationNumber("", "Portugal"));
    }

    @Test
    void ensureBlankRegistrationCountryIsRejected() {
        assertThrows(Exception.class, () -> new RegistrationNumber("CS-TFG", ""));
    }

    @Test
    void ensureRegistrationNumberIsNormalisedToUpperCase() {
        final var rn = new RegistrationNumber("cs-tfg", "Portugal");
        assertEquals("CS-TFG", rn.number());
    }

    // ── SeatClass VO ──────────────────────────────────────────────────────────

    @Test
    void ensureZeroSeatsInSeatClassIsRejected() {
        // US070.5: at least one seat with positive count
        assertThrows(Exception.class, () -> new SeatClass("Economy", 0));
    }

    @Test
    void ensureNegativeSeatsInSeatClassIsRejected() {
        assertThrows(Exception.class, () -> new SeatClass("Business", -5));
    }

    @Test
    void ensureBlankSeatClassNameIsRejected() {
        assertThrows(Exception.class, () -> new SeatClass("", 100));
    }

    // ── CabinConfiguration VO ────────────────────────────────────────────────

    @Test
    void ensureEmptyCabinConfigurationIsRejected() {
        // US070.5: at least one seat class
        assertThrows(Exception.class,
                () -> new CabinConfiguration(Collections.emptyList()));
    }

    @Test
    void ensureNullCabinConfigurationListIsRejected() {
        assertThrows(Exception.class, () -> new CabinConfiguration(null));
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/aircraft_test.csv", numLinesToSkip = 1, nullValues = {"N/A"})
    void ensureAircraftCsvInvariants(final String testCaseId, final String registration,
                                      final String country, final String modelCode,
                                      final String companyIata, final int crewMembers,
                                      final boolean expectedValid) {
        final var reg = (registration == null || registration.isBlank() || country == null || country.isBlank())
                ? null : new RegistrationNumber(registration, country);
        final var mc = (modelCode == null || modelCode.isBlank()) ? null : new AircraftModelCode(modelCode);
        final var ci = (companyIata == null || companyIata.isBlank()) ? null : new CompanyIATA(companyIata);
        if (expectedValid) {
            assertDoesNotThrow(() -> new Aircraft(reg, mc, ci, crewMembers, cabin170(), REG_DATE));
        } else {
            assertThrows(Exception.class, () -> new Aircraft(reg, mc, ci, crewMembers, cabin170(), REG_DATE));
        }
    }
}
