package eapli.aisafe.weatherdata.application;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.time.LocalDate;

/**
 * Controller for US043 - Consult Weather Data
 * Actors: Weather Person, Pilot, Flight Control Operator.
 */
@UseCaseController
public class ConsultWeatherDataController {

    private final AuthorizationService authz;
    private final WeatherDataRepository repo;

    /** Production constructor - uses framework registries. */
    public ConsultWeatherDataController(){
        this(AuthzRegistry.authorizationService(), PersistenceContext.repositories().weatherData());
    }

    /** Testing constructor */
    ConsultWeatherDataController(final AuthorizationService authz, final WeatherDataRepository repo){
        this.authz = authz;
        this.repo = repo;
    }

    /**
     * Consult weather data for a given area code and date.
     *
     * @param areaCode the ACA area code
     * @param date the date to filter
     * @return matching weather data records
     */
    public Iterable<WeatherData> consultWeatherData(final String areaCode, final LocalDate date){
        authz.ensureAuthenticatedUserHasAnyOf(
                AISafeRoles.WEATHER_PERSON,
                AISafeRoles.PILOT,
                AISafeRoles.FLIGHT_CONTROL_OPERATOR);

        return repo.findByAreaCodeAndDate(AreaCode.valueOf(areaCode), date);
    }
}
