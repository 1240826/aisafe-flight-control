package eapli.aisafe.report.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.report.domain.MonthlyReport;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseMonthlyReportDataProviderTest {

    private static final YearMonth PERIOD = YearMonth.of(2026, 6);
    private static final LocalDateTime IN_PERIOD = LocalDateTime.of(2026, 6, 15, 10, 0);
    private static final LocalDateTime OUTSIDE_PERIOD = LocalDateTime.of(2026, 7, 1, 10, 0);
    private static final AreaCode FCO_AREA = new AreaCode("LPPC");

    private FlightRepository flightRepo;
    private WeatherDataRepository weatherRepo;
    private PilotRepository pilotRepo;
    private AircraftRepository aircraftRepo;
    private DatabaseMonthlyReportDataProvider provider;

    @BeforeEach
    void setUp() {
        flightRepo = mock(FlightRepository.class);
        weatherRepo = mock(WeatherDataRepository.class);
        pilotRepo = mock(PilotRepository.class);
        aircraftRepo = mock(AircraftRepository.class);
        provider = new DatabaseMonthlyReportDataProvider(flightRepo, weatherRepo,
                pilotRepo, aircraftRepo);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureGenerateForMonthAggregatesData() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), IN_PERIOD);
        flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "dsl1");
        flight.addFlightPlan(FlightPlanId.valueOf("FP002"), "dsl2");

        final Pilot activePilot = mock(Pilot.class);
        when(activePilot.isActive()).thenReturn(true);

        when(flightRepo.findAll()).thenReturn(List.of(flight));
        when(weatherRepo.findByAreaCode(FCO_AREA)).thenReturn(List.of());
        when(pilotRepo.findAll()).thenReturn(List.of(activePilot));
        when(aircraftRepo.findAll()).thenReturn(List.of(mock(Aircraft.class)));

        final MonthlyReport report = provider.generateForMonth(PERIOD, FCO_AREA);

        assertNotNull(report);
        assertEquals(PERIOD, report.period());
        assertEquals(1, report.totalFlights());
        assertEquals(2, report.totalFlightPlans());
        assertEquals(2, report.flightPlansDraft());
        assertEquals(0, report.totalWeatherRecords());
        assertEquals(1, report.totalActivePilots());
        assertEquals(1, report.totalAircraft());
    }

    @Test
    void ensureFiltersFlightsByMonth() {
        final Flight inMonth = new Flight(FlightDesignator.valueOf("TP1234"), IN_PERIOD);
        final Flight outOfMonth = new Flight(FlightDesignator.valueOf("TP5678"), OUTSIDE_PERIOD);

        inMonth.addFlightPlan(FlightPlanId.valueOf("FP001"), "dsl");
        outOfMonth.addFlightPlan(FlightPlanId.valueOf("FP002"), "dsl");

        when(flightRepo.findAll()).thenReturn(List.of(inMonth, outOfMonth));
        when(weatherRepo.findByAreaCode(FCO_AREA)).thenReturn(List.of());
        when(pilotRepo.findAll()).thenReturn(List.of());
        when(aircraftRepo.findAll()).thenReturn(List.of());

        final MonthlyReport report = provider.generateForMonth(PERIOD, FCO_AREA);

        assertEquals(1, report.totalFlights());
        assertEquals(1, report.totalFlightPlans());
    }

    @Test
    void ensureFiltersWeatherByMonthAndArea() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), IN_PERIOD);
        when(flightRepo.findAll()).thenReturn(List.of(flight));
        when(weatherRepo.findByAreaCode(FCO_AREA)).thenReturn(List.of());
        when(pilotRepo.findAll()).thenReturn(List.of());
        when(aircraftRepo.findAll()).thenReturn(List.of());

        final MonthlyReport report = provider.generateForMonth(PERIOD, FCO_AREA);

        assertEquals(0, report.totalWeatherRecords());
    }

    // ── Flight plan status breakdown ──────────────────────────────────────────

    @Test
    void ensureCountsFlightPlansByStatus() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), IN_PERIOD);
        final var fpDraft = flight.addFlightPlan(FlightPlanId.valueOf("FP001"), "dsl");
        final var fpInTest = flight.addFlightPlan(FlightPlanId.valueOf("FP002"), "dsl");
        fpInTest.markAsInTest();
        final var fpPassed = flight.addFlightPlan(FlightPlanId.valueOf("FP003"), "dsl");
        fpPassed.markAsInTest();
        fpPassed.markAsTestPassed();
        final var fpFailed = flight.addFlightPlan(FlightPlanId.valueOf("FP004"), "dsl");
        fpFailed.markAsInTest();
        fpFailed.markAsTestFailed();

        when(flightRepo.findAll()).thenReturn(List.of(flight));
        when(weatherRepo.findByAreaCode(FCO_AREA)).thenReturn(List.of());
        when(pilotRepo.findAll()).thenReturn(List.of());
        when(aircraftRepo.findAll()).thenReturn(List.of());

        final MonthlyReport report = provider.generateForMonth(PERIOD, FCO_AREA);

        assertEquals(4, report.totalFlightPlans());
        assertEquals(1, report.flightPlansDraft());
        assertEquals(1, report.flightPlansInTest());
        assertEquals(1, report.flightPlansPassed());
        assertEquals(1, report.flightPlansFailed());
    }

    // ── Pilot counting ────────────────────────────────────────────────────────

    @Test
    void ensureOnlyActivePilotsAreCounted() {
        final Flight flight = new Flight(FlightDesignator.valueOf("TP1234"), IN_PERIOD);
        final Pilot active = mock(Pilot.class);
        when(active.isActive()).thenReturn(true);
        final Pilot inactive = mock(Pilot.class);
        when(inactive.isActive()).thenReturn(false);

        when(flightRepo.findAll()).thenReturn(List.of(flight));
        when(weatherRepo.findByAreaCode(FCO_AREA)).thenReturn(List.of());
        when(pilotRepo.findAll()).thenReturn(List.of(active, inactive));
        when(aircraftRepo.findAll()).thenReturn(List.of());

        final MonthlyReport report = provider.generateForMonth(PERIOD, FCO_AREA);

        assertEquals(1, report.totalActivePilots());
    }

    // ── Empty data ────────────────────────────────────────────────────────────

    @Test
    void ensureEmptyReportWhenNoData() {
        when(flightRepo.findAll()).thenReturn(List.of());
        when(weatherRepo.findByAreaCode(FCO_AREA)).thenReturn(List.of());
        when(pilotRepo.findAll()).thenReturn(List.of());
        when(aircraftRepo.findAll()).thenReturn(List.of());

        final MonthlyReport report = provider.generateForMonth(PERIOD, FCO_AREA);

        assertEquals(0, report.totalFlights());
        assertEquals(0, report.totalFlightPlans());
        assertEquals(0, report.totalWeatherRecords());
        assertEquals(0, report.totalActivePilots());
        assertEquals(0, report.totalAircraft());
    }
}
