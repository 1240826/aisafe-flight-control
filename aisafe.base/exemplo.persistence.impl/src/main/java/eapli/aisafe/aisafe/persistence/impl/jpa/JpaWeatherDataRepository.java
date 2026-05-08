package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.weatherdata.domain.WeatherData;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.exemplo.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

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
}
