package eapli.aisafe.persistence.jpa;

import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.infrastructure.Application;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class JpaFlightRepository
        extends JpaAutoTxRepository<Flight, FlightDesignator, FlightDesignator>
        implements FlightRepository {

    public JpaFlightRepository(final TransactionalContext autoTx) {
        super(autoTx, "designator");
    }

    public JpaFlightRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "designator");
    }

    @Override
    public java.util.Optional<Flight> findByFlightPlanId(final FlightPlanId flightPlanId) {
        final var query = entityManager().createQuery(
                "SELECT f FROM Flight f JOIN f.flightPlans fp WHERE fp.flightPlanId.id = :id",
                Flight.class);
        query.setParameter("id", flightPlanId.toString());
        try {
            return java.util.Optional.of(query.getSingleResult());
        } catch (final jakarta.persistence.NoResultException e) {
            return java.util.Optional.empty();
        }
    }

    @Override
    public boolean existsByPilotLicense(final PilotId pilotId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("license", pilotId.licenseNumber());
        return matchOne("e.pilotLicense.licenseNumber = :license", params).isPresent();
    }

    @Override
    public boolean existsByRouteNameAndDepartureTimeAfter(final FlightRouteName routeName,
                                                          final LocalDateTime dateTime) {
        final Map<String, Object> params = new HashMap<>();
        params.put("routeName", routeName.name());
        params.put("dateTime", dateTime);
        return matchOne("e.routeName.name = :routeName AND e.departureTime >= :dateTime", params).isPresent();
    }
}
