package eapli.aisafe.report.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MonthlyReportTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("csvTestData")
    void ensureMonthlyReportFromCsv(
            final String testCaseId,
            final String periodStr,
            final long totalFlights,
            final long totalFlightPlans,
            final long flightPlansDraft,
            final long flightPlansInTest,
            final long flightPlansPassed,
            final long flightPlansFailed,
            final long totalWeatherRecords,
            final long totalActivePilots,
            final long totalAircraft,
            final String flightsPerWeek,
            final double expectedPassRate,
            final long expectedTestedPlans) {
        final var period = YearMonth.parse(periodStr);
        final var report = new MonthlyReport(period, totalFlights, totalFlightPlans,
                flightPlansDraft, flightPlansInTest, flightPlansPassed, flightPlansFailed,
                totalWeatherRecords, totalActivePilots, totalAircraft, flightsPerWeek);

        assertNotNull(report);
        assertEquals(period, report.period());
        assertEquals(totalFlights, report.totalFlights());
        assertEquals(totalFlightPlans, report.totalFlightPlans());
        assertEquals(flightPlansDraft, report.flightPlansDraft());
        assertEquals(flightPlansInTest, report.flightPlansInTest());
        assertEquals(flightPlansPassed, report.flightPlansPassed());
        assertEquals(flightPlansFailed, report.flightPlansFailed());
        assertEquals(totalWeatherRecords, report.totalWeatherRecords());
        assertEquals(totalActivePilots, report.totalActivePilots());
        assertEquals(totalAircraft, report.totalAircraft());
        assertEquals(flightsPerWeek, report.flightsPerWeek());
        assertEquals(expectedTestedPlans, report.testedFlightPlans());
        assertEquals(expectedPassRate, report.passRatePercent(), 0.01);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("csvTestData")
    void ensureToStringContainsPeriod(
            final String testCaseId,
            final String periodStr,
            final long totalFlights,
            final long totalFlightPlans,
            final long flightPlansDraft,
            final long flightPlansInTest,
            final long flightPlansPassed,
            final long flightPlansFailed,
            final long totalWeatherRecords,
            final long totalActivePilots,
            final long totalAircraft,
            final String flightsPerWeek,
            final double expectedPassRate,
            final long expectedTestedPlans) {
        final var period = YearMonth.parse(periodStr);
        final var report = new MonthlyReport(period, totalFlights, totalFlightPlans,
                flightPlansDraft, flightPlansInTest, flightPlansPassed, flightPlansFailed,
                totalWeatherRecords, totalActivePilots, totalAircraft, flightsPerWeek);
        final var str = report.toString();
        assertTrue(str.contains(periodStr));
        assertTrue(str.contains("MONTHLY REPORT"));
        assertTrue(str.contains("Flights"));
        assertTrue(str.contains("Flight Plans"));
        assertTrue(str.contains("Total Aircraft"));
    }

    private static Stream<Arguments> csvTestData() {
        final var rows = new ArrayList<Arguments>();
        try (var reader = new BufferedReader(new InputStreamReader(
                MonthlyReportTest.class.getResourceAsStream("/us112/monthly_report_test.csv"),
                StandardCharsets.UTF_8))) {
            final var lines = reader.lines().toList();
            for (final var line : lines) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("testCaseId")) continue;
                final var parts = line.split(",", -1);
                if (parts.length < 14) continue;
                rows.add(Arguments.of(
                        parts[0].trim(),
                        parts[1].trim(),
                        Long.parseLong(parts[2].trim()),
                        Long.parseLong(parts[3].trim()),
                        Long.parseLong(parts[4].trim()),
                        Long.parseLong(parts[5].trim()),
                        Long.parseLong(parts[6].trim()),
                        Long.parseLong(parts[7].trim()),
                        Long.parseLong(parts[8].trim()),
                        Long.parseLong(parts[9].trim()),
                        Long.parseLong(parts[10].trim()),
                        parts[11].trim(),
                        Double.parseDouble(parts[12].trim()),
                        Long.parseLong(parts[13].trim())
                ));
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to load CSV test data", e);
        }
        return rows.stream();
    }
}
