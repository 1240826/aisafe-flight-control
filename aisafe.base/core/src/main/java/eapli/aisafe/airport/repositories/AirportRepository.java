package eapli.aisafe.airport.repositories;

import eapli.aisafe.airport.domain.Airport;
import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.airport.domain.AirportICAO;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

/**
 * Repository for Airport aggregate.
 * US052.
 */
public interface AirportRepository extends DomainRepository<AirportIATA, Airport> {

    Optional<Airport> findByIcao(AirportICAO icao);
}
