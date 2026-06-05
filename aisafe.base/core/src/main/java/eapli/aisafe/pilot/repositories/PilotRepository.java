package eapli.aisafe.pilot.repositories;

import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

/**
 * Repository for Pilot aggregate.
 * US075, US076, US077.
 */
public interface PilotRepository extends DomainRepository<PilotId, Pilot> {

    /**
     * US075/077: find a pilot by license number.
     */
    Optional<Pilot> findByLicenseNumber(PilotId pilotId);

    /**
     * US076: list all pilots (active + inactive) for a given company.
     */
    Iterable<Pilot> findByCompany(CompanyIATA company);

    /**
     * US076: list only active pilots for a given company.
     */
    Iterable<Pilot> findActiveByCompany(CompanyIATA company);

    /**
     * US077: check if any pilot for this license number has flight plans assigned.
     * Delegated to FlightRepository in the controller.
     */
    boolean hasAssignedFlights(PilotId pilotId);
}
