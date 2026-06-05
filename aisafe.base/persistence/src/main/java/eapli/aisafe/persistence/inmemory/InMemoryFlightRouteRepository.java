package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class InMemoryFlightRouteRepository
        extends InMemoryDomainRepository<FlightRoute, FlightRouteName>
        implements FlightRouteRepository {

    @Override
    public boolean existsByName(final FlightRouteName name) {
        return matchOne(r -> r.identity().equals(name)).isPresent();
    }

    @Override
    public Iterable<FlightRoute> findAllActive() {
        return match(FlightRoute::isActive);
    }

    @Override
    public boolean hasPlannedFlightsAfter(final FlightRouteName name, final LocalDate date) {
        return false;
    }

    @Override
    public Iterable<FlightRoute> findByCompany(final CompanyIATA companyIATA) {
        return () -> StreamSupport.stream(findAll().spliterator(), false)
                .filter(r -> companyIATA.equals(r.companyIATA()))
                .iterator();
    }

    @Override
    public Optional<FlightRoute> findByOriginAndDestination(final AirportIATA origin,
                                                            final AirportIATA destination) {
        return StreamSupport.stream(findAll().spliterator(), false)
                .filter(FlightRoute::isActive)
                .filter(r -> origin.equals(r.origin()) && destination.equals(r.destination()))
                .findFirst();
    }

    @Override
    public Optional<FlightRoute> findByOriginAndDestinationAndCompany(final AirportIATA origin,
                                                                       final AirportIATA destination,
                                                                       final CompanyIATA company) {
        return StreamSupport.stream(findAll().spliterator(), false)
                .filter(FlightRoute::isActive)
                .filter(r -> origin.equals(r.origin()) && destination.equals(r.destination()))
                .filter(r -> company.equals(r.companyIATA()))
                .findFirst();
    }
}