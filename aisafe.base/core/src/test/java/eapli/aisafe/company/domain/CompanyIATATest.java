package eapli.aisafe.company.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

class CompanyIATATest {

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/company_iata_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureCompanyIATAInvariants(
            final String testCaseId,
            final String iataCode,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            assertDoesNotThrow(() -> new CompanyIATA(iataCode));
        } else {
            assertThrows(Exception.class, () -> new CompanyIATA(iataCode));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/company_iata_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureCompanyIATAEquals(
            final String testCaseId,
            final String iataCode,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final CompanyIATA iata1 = new CompanyIATA(iataCode);
            final CompanyIATA iata2 = new CompanyIATA(iataCode);
            assertEquals(iata1, iata2);
            assertEquals(iata1.hashCode(), iata2.hashCode());
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/company_iata_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureCompanyIATAToString(
            final String testCaseId,
            final String iataCode,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final CompanyIATA iata = new CompanyIATA(iataCode);
            assertNotNull(iata.toString());
            assertTrue(iata.toString().contains(iataCode.toUpperCase()));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/company_iata_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureCompanyIATAGetters(
            final String testCaseId,
            final String iataCode,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final CompanyIATA iata = new CompanyIATA(iataCode);
            assertNotNull(iata.toString());
            assertEquals(iataCode.toUpperCase(), iata.toString());
        }
    }
}