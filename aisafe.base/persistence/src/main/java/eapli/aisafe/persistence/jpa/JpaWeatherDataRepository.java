package eapli.aisafe.persistence.jpa;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.aisafe.infrastructure.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA implementation of WeatherDataRepository.
 */
public class JpaWeatherDataRepository
        extends JpaAutoTxRepository<WeatherData, Long, Long>
        implements WeatherDataRepository {

    public JpaWeatherDataRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaWeatherDataRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "id");
    }

    @Override
    public Iterable<WeatherData> findByAreaCode(final AreaCode areaCode) {
        return match("e.areaCode.code = '" + areaCode.toString() + "'");
    }

    @Override
    public Iterable<WeatherData> findByAreaCodeAndDate(final AreaCode areaCode, final LocalDate date){
        final Map<String, Object> params = new HashMap<>();
        params.put("areaCode", areaCode.toString());
        params.put("start", date.atStartOfDay());
        params.put("end", date.atTime(LocalTime.MAX));
        return match("e.areaCode.code = :areaCode AND e.recordedDateTime BETWEEN :start AND :end", params);    }
}
