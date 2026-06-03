package eapli.aisafe.flightroute.repositories;

import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.framework.domain.repositories.DomainRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository for FlightRoute aggregate.
 * US073, US074, US080.
 */
public interface FlightRouteRepository extends DomainRepository<FlightRouteName, FlightRoute> {

    boolean existsByName(FlightRouteName name);

    Iterable<FlightRoute> findAllActive();

    boolean hasPlannedFlightsAfter(FlightRouteName name, LocalDate date);

    Iterable<FlightRoute> findByCompany(CompanyIATA companyIATA);

    /** US080/081: find an active route matching origin and destination airports. */
    Optional<FlightRoute> findByOriginAndDestination(AirportIATA origin, AirportIATA destination);

    /** US080/081: find an active route matching origin, destination, and company. */
    Optional<FlightRoute> findByOriginAndDestinationAndCompany(AirportIATA origin, AirportIATA destination, CompanyIATA company);
}