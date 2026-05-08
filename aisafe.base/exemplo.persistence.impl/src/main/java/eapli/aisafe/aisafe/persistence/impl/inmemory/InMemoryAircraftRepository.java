package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.Optional;
import java.util.stream.StreamSupport;

public class InMemoryAircraftRepository
        extends InMemoryDomainRepository<Aircraft, RegistrationNumber>
        implements AircraftRepository {

    @Override
    public Optional<Aircraft> findByRegistrationNumber(final RegistrationNumber registrationNumber) {
        return ofIdentity(registrationNumber);
    }

    @Override
    public Iterable<Aircraft> findAllActive() {
        return () -> StreamSupport.stream(findAll().spliterator(), false)
                .filter(Aircraft::isActive)
                .iterator();
    }

    @Override
    public Iterable<Aircraft> findByCompanyId(final CompanyIATA companyId) {
        return () -> StreamSupport.stream(findAll().spliterator(), false)
                .filter(a -> a.companyId().equals(companyId))
                .iterator();
    }
}
