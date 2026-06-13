package eapli.aisafe.company.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

class AirTransportCompanyTest {

    @Test
    void ensureValidCompanyCanBeCreated() {
        final var iata = CompanyIATA.valueOf("TP");
        final var icao = new CompanyICAO("TAP");
        final var company = new AirTransportCompany(iata, icao, "TAP Air Portugal");
        assertEquals(iata, company.identity());
        assertEquals(iata, company.iata());
        assertEquals(icao, company.icao());
        assertEquals("TAP Air Portugal", company.name());
    }

    @Test
    void ensureNameMustNotBeBlank() {
        final var iata = CompanyIATA.valueOf("TP");
        final var icao = new CompanyICAO("TAP");
        assertThrows(Exception.class, () -> new AirTransportCompany(iata, icao, ""));
        assertThrows(Exception.class, () -> new AirTransportCompany(iata, icao, "   "));
    }

    @Test
    void ensureNameMustContainAtLeastOneLetter() {
        final var iata = CompanyIATA.valueOf("TP");
        final var icao = new CompanyICAO("TAP");
        assertThrows(Exception.class, () -> new AirTransportCompany(iata, icao, "12345"));
    }

    @Test
    void ensureNullIataIsRejected() {
        final var icao = new CompanyICAO("TAP");
        assertThrows(Exception.class, () -> new AirTransportCompany(null, icao, "Test"));
    }

    @Test
    void ensureNullIcaoIsRejected() {
        final var iata = CompanyIATA.valueOf("TP");
        assertThrows(Exception.class, () -> new AirTransportCompany(iata, null, "Test"));
    }

    @Test
    void ensureNullNameIsRejected() {
        final var iata = CompanyIATA.valueOf("TP");
        final var icao = new CompanyICAO("TAP");
        assertThrows(Exception.class, () -> new AirTransportCompany(iata, icao, null));
    }

    @Test
    void ensureNameIsTrimmed() {
        final var iata = CompanyIATA.valueOf("RY");
        final var icao = new CompanyICAO("RYR");
        final var company = new AirTransportCompany(iata, icao, "  Ryanair  ");
        assertEquals("Ryanair", company.name());
    }

    @Test
    void ensureCompaniesWithSameIataAreEqual() {
        final var iata = CompanyIATA.valueOf("TP");
        final var icao = new CompanyICAO("TAP");
        final var c1 = new AirTransportCompany(iata, icao, "TAP Air Portugal");
        final var c2 = new AirTransportCompany(iata, new CompanyICAO("TAP"), "Different Name");
        assertEquals(c1, c2, "Equality based on IATA identity");
    }

    @Test
    void ensureCompaniesWithDifferentIataAreNotEqual() {
        final var c1 = new AirTransportCompany(CompanyIATA.valueOf("TP"), new CompanyICAO("TAP"), "TAP");
        final var c2 = new AirTransportCompany(CompanyIATA.valueOf("RY"), new CompanyICAO("RYR"), "Ryanair");
        assertNotEquals(c1, c2);
    }

    @Test
    void ensureToStringContainsIataIcaoAndName() {
        final var company = new AirTransportCompany(
                CompanyIATA.valueOf("TP"), new CompanyICAO("TAP"), "TAP Air Portugal");
        final var s = company.toString();
        assertTrue(s.contains("TP"));
        assertTrue(s.contains("TAP"));
        assertTrue(s.contains("TAP Air Portugal"));
    }

    @ParameterizedTest(name = "{0}: iata={1} icao={2} name={3} expectedValid={4}")
    @CsvFileSource(resources = "/us061/air_transport_company_test.csv", numLinesToSkip = 1)
    void ensureCompanyCsvInvariants(
            final String testCaseId, final String iataCode,
            final String icaoCode, final String name, final boolean expectedValid) {
        if (expectedValid) {
            assertDoesNotThrow(() -> {
                final var iata = CompanyIATA.valueOf(iataCode);
                final var icao = new CompanyICAO(icaoCode);
                new AirTransportCompany(iata, icao, name);
            });
        } else {
            assertThrows(Exception.class, () -> {
                final var iata = (iataCode == null || iataCode.isBlank())
                        ? null : CompanyIATA.valueOf(iataCode);
                final var icao = (icaoCode == null || icaoCode.isBlank())
                        ? null : new CompanyICAO(icaoCode);
                new AirTransportCompany(iata, icao, name);
            });
        }
    }
}
