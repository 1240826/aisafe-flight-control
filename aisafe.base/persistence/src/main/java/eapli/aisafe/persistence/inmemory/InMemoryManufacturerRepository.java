package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.aisafe.manufacturer.domain.ManufacturerName;
import eapli.aisafe.manufacturer.repositories.ManufacturerRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.Optional;

/**
 * InMemory implementation of ManufacturerRepository.
 */
public class InMemoryManufacturerRepository
        extends InMemoryDomainRepository<Manufacturer, ManufacturerName>
        implements ManufacturerRepository {

    @Override
    public Optional<Manufacturer> findByNameIgnoreCase(final String name) {
        return matchOne(m -> m.name().toString().equalsIgnoreCase(name));
    }
}
