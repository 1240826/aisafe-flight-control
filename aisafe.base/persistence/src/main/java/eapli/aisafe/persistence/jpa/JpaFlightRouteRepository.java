package eapli.aisafe.persistence.jpa;

import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.flightroute.domain.FlightRoute;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.aisafe.infrastructure.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.time.LocalDate;

/**
 * JPA implementation of FlightRouteRepository.
 * US073, US074.
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
        return matchOne("e.name.name = '" + name.name() + "'").isPresent();
    }

    @Override
    public Iterable<FlightRoute> findAllActive() {
        return match("e.deactivationDate IS NULL");
    }

    @Override
    public boolean hasPlannedFlightsAfter(final FlightRouteName name, final LocalDate date) {
        // TODO: implement when Flight aggregate exists
        return false;
    }

    @Override
    public Iterable<FlightRoute> findByCompany(final CompanyIATA companyIATA) {
        return match("e.companyIATA.iataCode = '" + companyIATA + "'");    }
}