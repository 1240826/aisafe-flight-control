package eapli.aisafe.flight.repositories;

import eapli.aisafe.flight.domain.Flight;
import eapli.aisafe.flight.domain.FlightDesignator;
import eapli.aisafe.flightplan.domain.FlightPlanId;
import eapli.aisafe.flightroute.domain.FlightRouteName;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.framework.domain.repositories.DomainRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FlightRepository extends DomainRepository<FlightDesignator, Flight> {

    Optional<Flight> findByFlightPlanId(FlightPlanId flightPlanId);

    /** US077: check if any flight has this pilot assigned. */
    boolean existsByPilotLicense(PilotId pilotId);

    /** US074: check if any flight exists on this route departing on or after the given date-time. */
    boolean existsByRouteNameAndDepartureTimeAfter(FlightRouteName routeName, LocalDateTime dateTime);
}
