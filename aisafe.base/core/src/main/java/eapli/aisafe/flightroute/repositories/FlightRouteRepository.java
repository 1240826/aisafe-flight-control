package eapli.aisafe.flightroute.repositories;

import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.framework.domain.repositories.DomainRepository;

import java.time.LocalDate;

/**
 * Repository for FlightRoute aggregate.
 * US073, US074.
 */
public interface FlightRouteRepository extends DomainRepository<FlightRouteName, FlightRoute> {

    boolean existsByName(FlightRouteName name);

    Iterable<FlightRoute> findAllActive();

    boolean hasPlannedFlightsAfter(FlightRouteName name, LocalDate date);

    Iterable<FlightRoute> findByCompany(CompanyIATA companyIATA);
}