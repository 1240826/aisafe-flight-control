package eapli.aisafe.report.domain;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class MonthlyReportTest {

    private static final YearMonth PERIOD = YearMonth.of(2026, 6);

    private MonthlyReport validReport() {
        return new MonthlyReport(PERIOD, 10, 25, 5, 3, 15, 2, 100, 8, 12, "");
    }

    @Test
    void ensureMonthlyReportIsCreated() {
        final var report = validReport();
        assertNotNull(report);
    }

    @Test
    void ensurePeriodIsPreserved() {
        final var report = validReport();
        assertEquals(PERIOD, report.period());
    }

    @Test
    void ensureTotalFlightsIsPreserved() {
        final var report = validReport();
        assertEquals(10, report.totalFlights());
    }

    @Test
    void ensureTotalFlightPlansIsPreserved() {
        final var report = validReport();
        assertEquals(25, report.totalFlightPlans());
    }

    @Test
    void ensureFlightPlansBreakdownIsPreserved() {
        final var report = validReport();
        assertEquals(5, report.flightPlansDraft());
        assertEquals(3, report.flightPlansInTest());
        assertEquals(15, report.flightPlansPassed());
        assertEquals(2, report.flightPlansFailed());
    }

    @Test
    void ensureWeatherRecordsIsPreserved() {
        final var report = validReport();
        assertEquals(100, report.totalWeatherRecords());
    }

    @Test
    void ensureActivePilotsIsPreserved() {
        final var report = validReport();
        assertEquals(8, report.totalActivePilots());
    }

    @Test
    void ensureTotalAircraftIsPreserved() {
        final var report = validReport();
        assertEquals(12, report.totalAircraft());
    }

    @Test
    void ensureAllZerosCanBePassed() {
        final var report = new MonthlyReport(PERIOD, 0, 0, 0, 0, 0, 0, 0, 0, 0, "");
        assertEquals(0, report.totalFlights());
        assertEquals(0, report.totalAircraft());
    }

    @Test
    void ensureLargeValuesAreStored() {
        final var report = new MonthlyReport(PERIOD, 1_000_000, 5_000_000,
                1_000_000, 1_000_000, 2_000_000, 1_000_000,
                500_000, 10_000, 500, "");
        assertEquals(1_000_000, report.totalFlights());
        assertEquals(500, report.totalAircraft());
    }

    @Test
    void ensureToStringContainsPeriod() {
        final var report = validReport();
        assertTrue(report.toString().contains("2026-06"));
    }

    @Test
    void ensureToStringContainsAllSections() {
        final var report = validReport();
        final String str = report.toString();
        assertTrue(str.contains("MONTHLY REPORT"));
        assertTrue(str.contains("Flights"));
        assertTrue(str.contains("Flight Plans"));
        assertTrue(str.contains("DRAFT"));
        assertTrue(str.contains("IN TEST"));
        assertTrue(str.contains("TEST PASSED"));
        assertTrue(str.contains("TEST FAILED"));
        assertTrue(str.contains("Weather Records"));
        assertTrue(str.contains("Active Pilots"));
        assertTrue(str.contains("Total Aircraft"));
    }
}
