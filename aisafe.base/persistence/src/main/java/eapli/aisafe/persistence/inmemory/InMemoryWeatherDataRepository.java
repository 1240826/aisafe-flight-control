package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainAutoNumberRepository;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.LocalDate;

public class InMemoryWeatherDataRepository
        extends InMemoryDomainAutoNumberRepository<WeatherData>
        implements WeatherDataRepository {

    @Override
    public Iterable<WeatherData> findByAreaCode(final AreaCode areaCode) {
        return match(w -> w.areaCode().equals(areaCode));
    }

    @Override
    public Iterable <WeatherData> findByAreaCodeAndDate(final AreaCode areaCode, final LocalDate date) {
        final LocalDateTime start = date.atStartOfDay();
        final LocalDateTime end = date.atTime(LocalTime.MAX);
        return match(w -> w.areaCode().equals(areaCode) && !w.recordedDateTime().isBefore(start) && !w.recordedDateTime().isAfter(end));
    }

}
