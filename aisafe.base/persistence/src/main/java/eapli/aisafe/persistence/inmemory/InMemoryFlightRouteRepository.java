package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.time.LocalDate;

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
        // TODO: implement when Flight aggregate exists
        return false;
    }

    @Override
    public Iterable<FlightRoute> findByCompany(final CompanyIATA companyIATA) {
        // TODO: implement when FlightRoute domain is complete
        return match(r -> false);
    }
}