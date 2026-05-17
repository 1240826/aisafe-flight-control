package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
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

    @Override
    public Iterable<Aircraft> findActiveByAircraftModelCode(final AircraftModelCode modelCode) {
        return () -> StreamSupport.stream(findAll().spliterator(), false)
                .filter(Aircraft::isActive)
                .filter(a -> a.aircraftModelCode().equals(modelCode))
                .iterator();
    }

    @Override
    public Iterable<Aircraft> findByCompanyIdAndModel(final CompanyIATA companyId,
                                                      final AircraftModelCode modelCode) {
        return () -> StreamSupport.stream(findAll().spliterator(), false)
                .filter(a -> a.companyId().equals(companyId))
                .filter(a -> a.aircraftModelCode().equals(modelCode))
                .iterator();
    }

    @Override
    public Iterable<Aircraft> findByCompanyIdAndMaker(final CompanyIATA companyId,
                                                      final String makerName) {
        return () -> StreamSupport.stream(findAll().spliterator(), false)
                .filter(a -> a.companyId().equals(companyId))
                .filter(a -> a.aircraftModelCode().toString()
                        .toLowerCase().contains(makerName.toLowerCase()))
                .iterator();
    }

    @Override
    public Iterable<Aircraft> findByCompanyIdAndMinCapacity(final CompanyIATA companyId,
                                                            final int minCapacity) {
        return () -> StreamSupport.stream(findAll().spliterator(), false)
                .filter(a -> a.companyId().equals(companyId))
                .filter(a -> a.totalCapacity() >= minCapacity)
                .iterator();
    }
}
