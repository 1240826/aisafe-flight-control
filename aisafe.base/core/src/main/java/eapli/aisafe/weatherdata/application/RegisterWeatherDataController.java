package eapli.aisafe.weatherdata.application;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.domain.WindCondition;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.time.LocalDateTime;

/**
 * Controller for US041 — Register Weather Data for an ACA.
 * US042: data may come from multiple providers (sourceProvider field).
 * Actor: Weather Person.
 */
@UseCaseController
public class RegisterWeatherDataController {

    private final AuthorizationService authz;
    private final WeatherDataRepository repo;
    private final AirControlAreaRepository acaRepo;

    /** Production constructor — uses framework registries. */
    public RegisterWeatherDataController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().airControlAreas());
    }

    /** Testing constructor — allows injecting mocks. */
    RegisterWeatherDataController(final AuthorizationService authz,
                                   final WeatherDataRepository repo,
                                   final AirControlAreaRepository acaRepo) {
        this.authz = authz;
        this.repo = repo;
        this.acaRepo = acaRepo;
    }

    /**
     * Register a weather observation at a specific coordinate within an ACA.
     *
     * @param areaCode            code of the ACA this observation belongs to
     * @param latitude            latitude of the observation point (-90 to 90)
     * @param longitude           longitude of the observation point (-180 to 180)
     * @param altitudeMetres      altitude of the observation in metres (>= 0)
     * @param windSpeedKnots      wind speed in knots (must be > 0)
     * @param windDirectionDeg    wind direction in degrees (0 to 360, exclusive)
     * @param temperatureCelsius  air temperature in degrees Celsius
     * @param sourceProvider      name/identifier of the data provider (e.g. "IPMA", "METAR LPPC")
     * @param recordedDateTime    the instant at which the observation was recorded
     * @return the saved WeatherData
     */
    public WeatherData registerWeatherData(final String areaCode,
                                            final double latitude, final double longitude,
                                            final int altitudeMetres,
                                            final double windSpeedKnots, final double windDirectionDeg,
                                            final double temperatureCelsius,
                                            final String sourceProvider,
                                            final LocalDateTime recordedDateTime) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.WEATHER_PERSON);

        acaRepo.ofIdentity(AreaCode.valueOf(areaCode))
                .orElseThrow(() -> new IllegalArgumentException("Air Control Area not found: " + areaCode));

        final WeatherData weatherData = new WeatherData(
                AreaCode.valueOf(areaCode),
                new WindCondition(windSpeedKnots, (int) windDirectionDeg,
                        latitude, longitude, altitudeMetres),
                temperatureCelsius,
                sourceProvider,
                recordedDateTime);

        return repo.save(weatherData);
    }

    /** Support method: list ACAs for selection. */
    public Iterable<AirControlArea> allAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.WEATHER_PERSON);
        return acaRepo.findAll();
    }

    /** List weather data for a given area. */
    public Iterable<WeatherData> weatherDataForArea(final String areaCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.WEATHER_PERSON, AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return repo.findByAreaCode(AreaCode.valueOf(areaCode));
    }
}
