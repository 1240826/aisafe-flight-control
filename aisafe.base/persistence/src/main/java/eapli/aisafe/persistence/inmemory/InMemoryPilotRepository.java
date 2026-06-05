package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.Optional;
import java.util.stream.StreamSupport;

public class InMemoryPilotRepository
        extends InMemoryDomainRepository<Pilot, PilotId>
        implements PilotRepository {

    @Override
    public Optional<Pilot> findByLicenseNumber(final PilotId pilotId) {
        return ofIdentity(pilotId);
    }

    @Override
    public Iterable<Pilot> findByCompany(final CompanyIATA company) {
        return () -> StreamSupport.stream(findAll().spliterator(), false)
                .filter(p -> company.equals(p.company()))
                .iterator();
    }

    @Override
    public Iterable<Pilot> findActiveByCompany(final CompanyIATA company) {
        return () -> StreamSupport.stream(findAll().spliterator(), false)
                .filter(p -> company.equals(p.company()))
                .filter(Pilot::isActive)
                .iterator();
    }

    @Override
    public boolean hasAssignedFlights(final PilotId pilotId) {
        // In-memory: no cross-aggregate reference, always returns false
        return false;
    }
}
