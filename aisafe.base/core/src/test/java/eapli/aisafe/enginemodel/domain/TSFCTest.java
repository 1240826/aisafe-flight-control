package eapli.aisafe.enginemodel.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

class TSFCTest {

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/tsfc_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureTSFCInvariants(
            final String testCaseId,
            final Double tsfcValue,
            final String units,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            assertDoesNotThrow(() -> new TSFC(tsfcValue, units));
        } else {
            assertThrows(Exception.class, () -> new TSFC(tsfcValue, units));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/tsfc_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureTSFCEquals(
            final String testCaseId,
            final Double tsfcValue,
            final String units,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final TSFC tsfc1 = new TSFC(tsfcValue, units);
            final TSFC tsfc2 = new TSFC(tsfcValue, units);
            assertEquals(tsfc1, tsfc2);
            assertEquals(tsfc1.hashCode(), tsfc2.hashCode());
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/tsfc_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureTSFCToString(
            final String testCaseId,
            final Double tsfcValue,
            final String units,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final TSFC tsfc = new TSFC(tsfcValue, units);
            assertNotNull(tsfc.toString());
            assertTrue(tsfc.toString().contains(String.valueOf(tsfcValue.intValue())));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/tsfc_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureTSFCGetters(
            final String testCaseId,
            final Double tsfcValue,
            final String units,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final TSFC tsfc = new TSFC(tsfcValue, units);
            assertNotNull(tsfc.value());
            assertNotNull(tsfc.unit());
        }
    }
}