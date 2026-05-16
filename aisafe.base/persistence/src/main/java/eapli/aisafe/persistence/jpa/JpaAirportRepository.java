package eapli.aisafe.persistence.jpa;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.airport.domain.AirportICAO;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.infrastructure.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.Optional;

/**
 * JPA implementation of AirportRepository.
 */
public class JpaAirportRepository
        extends JpaAutoTxRepository<Airport, AirportIATA, AirportIATA>
        implements AirportRepository {

    public JpaAirportRepository(final TransactionalContext autoTx) {
        super(autoTx, "iata");
    }

    public JpaAirportRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "iata");
    }

    @Override
    public Optional<Airport> findByIcao(final AirportICAO icao) {
        return matchOne("e.icao.icaoCode = '" + icao.toString() + "'");
    }
}
