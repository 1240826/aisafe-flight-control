package eapli.aisafe.weatherdata.repositories;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.framework.domain.repositories.DomainRepository;

import java.time.LocalDate;

/**
 * Repository for WeatherData aggregate.
 * US041.
 */
public interface WeatherDataRepository extends DomainRepository<Long, WeatherData> {

    Iterable<WeatherData> findByAreaCode(AreaCode areaCode);

    Iterable<WeatherData> findByAreaCodeAndDate(AreaCode areaCode, LocalDate date);
}
