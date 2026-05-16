package eapli.aisafe.manufacturer.repositories;

import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.aisafe.manufacturer.domain.ManufacturerName;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

/**
 * Repository for Manufacturer aggregate.
 * US055.
 */
public interface ManufacturerRepository extends DomainRepository<ManufacturerName, Manufacturer> {

    /**
     * Case-insensitive search by name.
     */
    Optional<Manufacturer> findByNameIgnoreCase(String name);
}
