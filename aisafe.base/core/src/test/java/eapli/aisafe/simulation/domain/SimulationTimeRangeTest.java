package eapli.aisafe.simulation.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class SimulationTimeRangeTest {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us070/time_range_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureSimulationTimeRangeInvariants(
            final String testCaseId,
            final String startDate,
            final String startTime,
            final String endDate,
            final String endTime,
            final boolean expectedValid
    ) {
        final LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T" + startTime);
        final LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T" + endTime);

        if (expectedValid) {
            assertDoesNotThrow(() -> new SimulationTimeRange(startDateTime, endDateTime));
        } else {
            assertThrows(Exception.class, () -> new SimulationTimeRange(startDateTime, endDateTime));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us070/time_range_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureSimulationTimeRangeEquals(
            final String testCaseId,
            final String startDate,
            final String startTime,
            final String endDate,
            final String endTime,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T" + startTime);
            final LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T" + endTime);
            final SimulationTimeRange range1 = new SimulationTimeRange(startDateTime, endDateTime);
            final SimulationTimeRange range2 = new SimulationTimeRange(startDateTime, endDateTime);
            assertEquals(range1, range2);
            assertEquals(range1.hashCode(), range2.hashCode());
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us070/time_range_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureSimulationTimeRangeToString(
            final String testCaseId,
            final String startDate,
            final String startTime,
            final String endDate,
            final String endTime,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T" + startTime);
            final LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T" + endTime);
            final SimulationTimeRange range = new SimulationTimeRange(startDateTime, endDateTime);
            assertNotNull(range.toString());
            assertTrue(range.toString().contains("→"));
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/us070/time_range_test.csv", numLinesToSkip = 1, nullValues = {"NULL", ""})
    void ensureSimulationTimeRangeGetters(
            final String testCaseId,
            final String startDate,
            final String startTime,
            final String endDate,
            final String endTime,
            final boolean expectedValid
    ) {
        if (expectedValid) {
            final LocalDateTime startDateTime = LocalDateTime.parse(startDate + "T" + startTime);
            final LocalDateTime endDateTime = LocalDateTime.parse(endDate + "T" + endTime);
            final SimulationTimeRange range = new SimulationTimeRange(startDateTime, endDateTime);
            assertNotNull(range.startDateTime());
            assertNotNull(range.endDateTime());
        }
    }
}