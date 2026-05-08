package eapli.aisafe.weatherdata.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.domain.WeatherSubArea;
import eapli.aisafe.weatherdata.domain.WindCondition;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.time.LocalDateTime;

/**
 * Controller for US041 — Register Weather Data for an ACA.
 * Actor: Weather Person.
 */
@UseCaseController
public class RegisterWeatherDataController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final WeatherDataRepository repo = PersistenceContext.repositories().weatherData();

    /**
     * Register weather data for an air control area.
     *
     * @param areaCode          code of the ACA
     * @param minLat            sub-area min latitude
     * @param maxLat            sub-area max latitude
     * @param minLon            sub-area min longitude
     * @param maxLon            sub-area max longitude
     * @param minAlt            sub-area min altitude (m)
     * @param maxAlt            sub-area max altitude (m)
     * @param windSpeedKnots    wind speed in knots
     * @param windDirectionDeg  wind direction in degrees
     * @param temperatureCelsius temperature in Celsius
     * @param validFrom         start of validity period
     * @param validTo           end of validity period
     * @return the saved WeatherData
     */
    public WeatherData registerWeatherData(final String areaCode,
                                            final double minLat, final double maxLat,
                                            final double minLon, final double maxLon,
                                            final double minAlt, final double maxAlt,
                                            final double windSpeedKnots, final double windDirectionDeg,
                                            final double temperatureCelsius,
                                            final LocalDateTime validFrom, final LocalDateTime validTo) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.WEATHER_PERSON, AISafeRoles.BACKOFFICE_OPERATOR);

        final WeatherData weatherData = new WeatherData(
                AreaCode.valueOf(areaCode),
                new WeatherSubArea(minLat, maxLat, minLon, maxLon, (int) minAlt, (int) maxAlt),
                new WindCondition(windSpeedKnots, (int) windDirectionDeg),
                temperatureCelsius,
                validFrom, validTo);

        return repo.save(weatherData);
    }

    /** List weather data for a given area. */
    public Iterable<WeatherData> weatherDataForArea(final String areaCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.WEATHER_PERSON, AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);
        return repo.findByAreaCode(AreaCode.valueOf(areaCode));
    }
}
