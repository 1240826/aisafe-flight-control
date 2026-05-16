package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.airport.domain.AirportICAO;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.Optional;

public class InMemoryAirportRepository
        extends InMemoryDomainRepository<Airport, AirportIATA>
        implements AirportRepository {

    @Override
    public Optional<Airport> findByIcao(final AirportICAO icao) {
        return matchOne(a -> a.icao().equals(icao));
    }
}
