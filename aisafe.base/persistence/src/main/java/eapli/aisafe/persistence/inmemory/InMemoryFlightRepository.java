package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flight.repositories.FlightRepository;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.time.LocalDateTime;
import java.util.stream.StreamSupport;

public class InMemoryFlightRepository
        extends InMemoryDomainRepository<Flight, FlightDesignator>
        implements FlightRepository {

    @Override
    public java.util.Optional<Flight> findByFlightPlanId(final FlightPlanId flightPlanId) {
        return StreamSupport.stream(match(f -> f.flightPlans().stream()
                .anyMatch(fp -> fp.identity().equals(flightPlanId)))
                .spliterator(), false).findFirst();
    }

    @Override
    public boolean existsByPilotLicense(final PilotId pilotId) {
        return StreamSupport.stream(findAll().spliterator(), false)
                .anyMatch(f -> pilotId.equals(f.pilotLicense()));
    }

    @Override
    public boolean existsByRouteNameAndDepartureTimeAfter(final FlightRouteName routeName,
                                                          final LocalDateTime dateTime) {
        return StreamSupport.stream(findAll().spliterator(), false)
                .anyMatch(f -> routeName.equals(f.routeName())
                        && !f.departureTime().isBefore(dateTime));
    }
}
