package eapli.aisafe.aircraftmodel.repositories;

import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;
import java.util.stream.StreamSupport;
import eapli.aisafe.manufacturer.domain.ManufacturerName;

/**
 * Repository for AircraftModel aggregate.
 * US055, US057, US058.
 */
public interface AircraftModelRepository extends DomainRepository<AircraftModelCode, AircraftModel> {

    /**
     * US055: uniqueness check — name + manufacturer must be unique.
     */
    Optional<AircraftModel> findByNameAndManufacturer(String name, String manufacturerName);


}
