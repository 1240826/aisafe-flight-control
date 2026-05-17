package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.Optional;
import java.util.stream.StreamSupport;


public class InMemoryAircraftModelRepository
        extends InMemoryDomainRepository<AircraftModel, AircraftModelCode>
        implements AircraftModelRepository {
    @Override
    public Optional<AircraftModel> findByNameAndManufacturer(final String name, final String manufacturerName) {
        return StreamSupport.stream(findAll().spliterator(), false)
                .filter(m -> m.name().equalsIgnoreCase(name)
                        && m.manufacturerName().equalsIgnoreCase(manufacturerName))
                .findFirst();
    }
}
