package eapli.aisafe.report.application;

import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.flightplan.domain.FlightPlanStatus;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.aisafe.report.domain.MonthlyReport;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.time.YearMonth;
import java.util.stream.StreamSupport;

@UseCaseController
public class GenerateMonthlyReportController {

    private final AuthorizationService authz;
    private final FlightRepository flightRepo;
    private final WeatherDataRepository weatherRepo;
    private final PilotRepository pilotRepo;
    private final AircraftRepository aircraftRepo;

    public GenerateMonthlyReportController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().aircraft());
    }

    GenerateMonthlyReportController(final AuthorizationService authz,
                                     final FlightRepository flightRepo,
                                     final WeatherDataRepository weatherRepo,
                                     final PilotRepository pilotRepo,
                                     final AircraftRepository aircraftRepo) {
        this.authz = authz;
        this.flightRepo = flightRepo;
        this.weatherRepo = weatherRepo;
        this.pilotRepo = pilotRepo;
        this.aircraftRepo = aircraftRepo;
    }

    public MonthlyReport generateForMonth(final YearMonth period) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.FLIGHT_CONTROL_OPERATOR);

        final Iterable<Flight> allFlights = flightRepo.findAll();
        final Iterable<WeatherData> allWeather = weatherRepo.findAll();
        final Iterable<Pilot> allPilots = pilotRepo.findAll();

        long totalFlights = 0;
        long totalFlightPlans = 0;
        long draft = 0;
        long inTest = 0;
        long passed = 0;
        long failed = 0;

        for (final Flight flight : allFlights) {
            if (isInPeriod(flight, period)) {
                totalFlights++;
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

        final long weatherInPeriod = StreamSupport.stream(allWeather.spliterator(), false)
                .filter(w -> YearMonth.from(w.recordedDateTime()).equals(period))
                .count();

        final long activePilots = StreamSupport.stream(allPilots.spliterator(), false)
                .filter(Pilot::isActive)
                .count();

        final long totalAircraft = StreamSupport.stream(
                aircraftRepo.findAll().spliterator(), false).count();

        return new MonthlyReport(period, totalFlights, totalFlightPlans,
                draft, inTest, passed, failed,
                weatherInPeriod, activePilots, totalAircraft);
    }

    private static boolean isInPeriod(final Flight flight, final YearMonth period) {
        return YearMonth.from(flight.departureTime()).equals(period);
    }
}
