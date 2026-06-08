package eapli.aisafe.report.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.report.domain.MonthlyReport;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;

import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DatabaseMonthlyReportDataProvider implements MonthlyReportDataProvider {

    private final FlightRepository flightRepo;
    private final WeatherDataRepository weatherRepo;
    private final PilotRepository pilotRepo;
    private final AircraftRepository aircraftRepo;

    public DatabaseMonthlyReportDataProvider(final FlightRepository flightRepo,
                                              final WeatherDataRepository weatherRepo,
                                              final PilotRepository pilotRepo,
                                              final AircraftRepository aircraftRepo) {
        this.flightRepo = flightRepo;
        this.weatherRepo = weatherRepo;
        this.pilotRepo = pilotRepo;
        this.aircraftRepo = aircraftRepo;
    }

    @Override
    public MonthlyReport generateForMonth(final YearMonth period, final AreaCode areaCode) {
        final Iterable<Flight> allFlights = flightRepo.findAll();
        final Iterable<Pilot> allPilots = pilotRepo.findAll();
        final Map<Integer, Long> flightsByWeek = new TreeMap<>();

        long totalFlights = 0;
        long totalFlightPlans = 0;
        long draft = 0;
        long inTest = 0;
        long passed = 0;
        long failed = 0;

        for (final Flight flight : allFlights) {
            if (isInPeriod(flight, period)) {
                totalFlights++;
                final int week = flight.departureTime().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                flightsByWeek.merge(week, 1L, Long::sum);
                for (final var fp : flight.flightPlans()) {
                    totalFlightPlans++;
                    switch (fp.status()) {
                        case DRAFT -> draft++;
                        case IN_TEST -> inTest++;
                        case TEST_PASSED -> passed++;
                        case TEST_FAILED -> failed++;
                    }
                }
            }
        }

        final long weatherInPeriod = StreamSupport.stream(
                weatherRepo.findByAreaCode(areaCode).spliterator(), false)
                .filter(w -> YearMonth.from(w.recordedDateTime()).equals(period))
                .count();

        final long activePilots = StreamSupport.stream(allPilots.spliterator(), false)
                .filter(Pilot::isActive)
                .count();

        final long totalAircraft = StreamSupport.stream(
                aircraftRepo.findAll().spliterator(), false).count();

        final String flightsPerWeek = flightsByWeek.entrySet().stream()
                .map(e -> "W" + e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(" | "));

        return new MonthlyReport(period, totalFlights, totalFlightPlans,
                draft, inTest, passed, failed,
                weatherInPeriod, activePilots, totalAircraft,
                flightsPerWeek);
    }

    private static boolean isInPeriod(final Flight flight, final YearMonth period) {
        return YearMonth.from(flight.departureTime()).equals(period);
    }
}
