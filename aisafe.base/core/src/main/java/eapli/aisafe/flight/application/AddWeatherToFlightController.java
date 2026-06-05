package eapli.aisafe.flight.application;

import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

@UseCaseController
public class AddWeatherToFlightController {

    private final AuthorizationService authz;
    private final FlightRepository flightRepo;
    private final WeatherDataRepository weatherRepo;

    public AddWeatherToFlightController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().weatherData());
    }

    AddWeatherToFlightController(final AuthorizationService authz,
                                  final FlightRepository flightRepo,
                                  final WeatherDataRepository weatherRepo) {
        this.authz = authz;
        this.flightRepo = flightRepo;
        this.weatherRepo = weatherRepo;
    }

    public Iterable<Flight> allFlights() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        return flightRepo.findAll();
    }

    public Flight flightByDesignator(final String designator) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        return flightRepo.ofIdentity(FlightDesignator.valueOf(designator))
                .orElseThrow(() -> new IllegalArgumentException("Flight not found: " + designator));
    }

    public Iterable<WeatherData> allWeatherData() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);
        return weatherRepo.findAll();
    }

    public Flight assignWeather(final String flightDesignator, final Long weatherDataId) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.PILOT);

        weatherRepo.ofIdentity(weatherDataId)
                .orElseThrow(() -> new IllegalArgumentException("Weather data not found: " + weatherDataId));

        final Flight flight = flightByDesignator(flightDesignator);
        flight.assignWeatherData(weatherDataId);
        return flightRepo.save(flight);
    }
}
