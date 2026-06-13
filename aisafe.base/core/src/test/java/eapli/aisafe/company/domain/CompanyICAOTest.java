package eapli.aisafe.company.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

class CompanyICAOTest {

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/company_icao_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureCompanyICAOINvariants(
            final String testCaseId,
            final String icaoCode,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            assertDoesNotThrow(() -> new CompanyICAO(icaoCode));
        } else {
            assertThrows(Exception.class, () -> new CompanyICAO(icaoCode));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/company_icao_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureCompanyICAOEquals(
            final String testCaseId,
            final String icaoCode,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final CompanyICAO icao1 = new CompanyICAO(icaoCode);
            final CompanyICAO icao2 = new CompanyICAO(icaoCode);
            assertEquals(icao1, icao2);
            assertEquals(icao1.hashCode(), icao2.hashCode());
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/company_icao_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureCompanyICAOToString(
            final String testCaseId,
            final String icaoCode,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final CompanyICAO icao = new CompanyICAO(icaoCode);
            assertNotNull(icao.toString());
            assertTrue(icao.toString().contains(icaoCode.toUpperCase()));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/company_icao_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureCompanyICAOGetters(
            final String testCaseId,
            final String icaoCode,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final CompanyICAO icao = new CompanyICAO(icaoCode);
            assertNotNull(icao.toString());
            assertEquals(icaoCode.toUpperCase(), icao.toString());
        }
    }
}