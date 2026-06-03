package eapli.aisafe.persistence.jpa;

import eapli.aisafe.airport.domain.AirportIATA;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.infrastructure.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of FlightRouteRepository.
 * US073, US074, US080.
 */
public class JpaFlightRouteRepository
        extends JpaAutoTxRepository<FlightRoute, FlightRouteName, FlightRouteName>
        implements FlightRouteRepository {

    public JpaFlightRouteRepository(final TransactionalContext autoTx) {
        super(autoTx, "name");
    }

    public JpaFlightRouteRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "name");
    }

    @Override
    public boolean existsByName(final FlightRouteName name) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", name.name());
        return matchOne("e.name.name = :name", params).isPresent();
    }

    @Override
    public Iterable<FlightRoute> findAllActive() {
        return match("e.deactivationDate IS NULL");
    }

    @Override
    public boolean hasPlannedFlightsAfter(final FlightRouteName name, final LocalDate date) {
        return false;
    }

    @Override
    public Iterable<FlightRoute> findByCompany(final CompanyIATA companyIATA) {
        final Map<String, Object> params = new HashMap<>();
        params.put("iata", companyIATA.toString());
        return match("e.companyIATA.iataCode = :iata", params);
    }

    @Override
    public Optional<FlightRoute> findByOriginAndDestination(final AirportIATA origin,
                                                            final AirportIATA destination) {
        final Map<String, Object> params = new HashMap<>();
        params.put("origin", origin.toString());
        params.put("dest", destination.toString());
        return matchOne("e.origin.iataCode = :origin AND e.destination.iataCode = :dest"
                + " AND e.deactivationDate IS NULL", params);
    }

    @Override
    public Optional<FlightRoute> findByOriginAndDestinationAndCompany(final AirportIATA origin,
                                                                       final AirportIATA destination,
                                                                       final CompanyIATA company) {
        final Map<String, Object> params = new HashMap<>();
        params.put("origin", origin.toString());
        params.put("dest", destination.toString());
        params.put("company", company.toString());
        return matchOne("e.origin.iataCode = :origin AND e.destination.iataCode = :dest"
                + " AND e.companyIATA.iataCode = :company"
                + " AND e.deactivationDate IS NULL", params);
    }
}