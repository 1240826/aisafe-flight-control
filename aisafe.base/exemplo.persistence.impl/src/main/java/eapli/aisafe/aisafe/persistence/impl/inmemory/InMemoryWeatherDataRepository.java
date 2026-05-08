package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainAutoNumberRepository;

public class InMemoryWeatherDataRepository
        extends InMemoryDomainAutoNumberRepository<WeatherData>
        implements WeatherDataRepository {

    @Override
    public Iterable<WeatherData> findByAreaCode(final AreaCode areaCode) {
        return match(w -> w.areaCode().equals(areaCode));
    }
}
