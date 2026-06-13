package eapli.aisafe.enginemodel.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

class PowerTest {

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/power_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensurePowerInvariants(
            final String testCaseId,
            final Double powerValue,
            final String units,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            assertDoesNotThrow(() -> new Power(powerValue, units));
        } else {
            assertThrows(Exception.class, () -> new Power(powerValue, units));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/power_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensurePowerEquals(
            final String testCaseId,
            final Double powerValue,
            final String units,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final Power p1 = new Power(powerValue, units);
            final Power p2 = new Power(powerValue, units);
            assertEquals(p1, p2);
            assertEquals(p1.hashCode(), p2.hashCode());
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/power_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensurePowerToString(
            final String testCaseId,
            final Double powerValue,
            final String units,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final Power p = new Power(powerValue, units);
            assertNotNull(p.toString());
            assertTrue(p.toString().contains(units));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us061/power_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensurePowerGetters(
            final String testCaseId,
            final Double powerValue,
            final String units,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final Power p = new Power(powerValue, units);
            assertNotNull(p.value());
            assertNotNull(p.unit());
        }
    }
}