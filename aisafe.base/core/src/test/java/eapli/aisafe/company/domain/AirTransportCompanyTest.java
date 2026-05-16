package eapli.aisafe.company.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AirTransportCompany aggregate root.
 * Covers US060 invariants: IATA uniqueness, ICAO uniqueness, name non-blank.
 */
class AirTransportCompanyTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static AirTransportCompany validCompany() {
        return new AirTransportCompany(
                new CompanyIATA("TP"),
                new CompanyICAO("TAP"),
                "TAP Air Portugal"
        );
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureValidCompanyCanBeCreated() {
        final var company = validCompany();
        assertNotNull(company);
        assertEquals("TP", company.iata().toString());
        assertEquals("TAP", company.icao().toString());
        assertEquals("TAP Air Portugal", company.name());
    }

    @Test
    void ensureIdentityReturnsIATA() {
        final var company = validCompany();
        assertEquals(new CompanyIATA("TP"), company.identity());
    }

    @Test
    void ensureCompanyWithLowercaseIATAIsNormalised() {
        // CompanyIATA should normalise to uppercase
        final var iata = new CompanyIATA("tp");
        assertEquals("TP", iata.toString());
    }

    @Test
    void ensureCompanyWithLowercaseICAOIsNormalised() {
        final var icao = new CompanyICAO("tap");
        assertEquals("TAP", icao.toString());
    }

    // ── Invariant violations ──────────────────────────────────────────────────

    @Test
    void ensureBlankNameIsRejected() {
        assertThrows(Exception.class, () -> new AirTransportCompany(
                new CompanyIATA("TP"),
                new CompanyICAO("TAP"),
                ""));
    }

    @Test
    void ensureWhitespaceOnlyNameIsRejected() {
        assertThrows(Exception.class, () -> new AirTransportCompany(
                new CompanyIATA("TP"),
                new CompanyICAO("TAP"),
                "   "));
    }

    @Test
    void ensureNullIATAIsRejected() {
        assertThrows(Exception.class, () -> new AirTransportCompany(
                null,
                new CompanyICAO("TAP"),
                "TAP Air Portugal"));
    }

    @Test
    void ensureNullICAOIsRejected() {
        assertThrows(Exception.class, () -> new AirTransportCompany(
                new CompanyIATA("TP"),
                null,
                "TAP Air Portugal"));
    }

    @Test
    void ensureNullNameIsRejected() {
        assertThrows(Exception.class, () -> new AirTransportCompany(
                new CompanyIATA("TP"),
                new CompanyICAO("TAP"),
                null));
    }

    // ── CompanyIATA VO ────────────────────────────────────────────────────────

    @Test
    void ensureCompanyIATARejectsBlank() {
        assertThrows(Exception.class, () -> new CompanyIATA(""));
    }

    @Test
    void ensureCompanyIATARejectsNull() {
        assertThrows(Exception.class, () -> new CompanyIATA(null));
    }

    // ── CompanyICAO VO ────────────────────────────────────────────────────────

    @Test
    void ensureCompanyICAORejectsBlank() {
        assertThrows(Exception.class, () -> new CompanyICAO(""));
    }

    @Test
    void ensureCompanyICAORejectsNull() {
        assertThrows(Exception.class, () -> new CompanyICAO(null));
    }

    // ── Equality ──────────────────────────────────────────────────────────────

    @Test
    void ensureCompaniesWithSameIATACodeAreEqual() {
        final var c1 = validCompany();
        final var c2 = new AirTransportCompany(
                new CompanyIATA("TP"),
                new CompanyICAO("TPT"),
                "Different Name");
        assertEquals(c1, c2, "Companies with same IATA are the same entity");
    }

    @Test
    void ensureCompaniesWithDifferentIATACodeAreNotEqual() {
        final var c1 = validCompany();
        final var c2 = new AirTransportCompany(
                new CompanyIATA("FR"),
                new CompanyICAO("RYR"),
                "Ryanair");
        assertNotEquals(c1, c2);
    }
}
