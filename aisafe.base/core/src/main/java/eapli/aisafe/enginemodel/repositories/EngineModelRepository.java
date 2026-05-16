package eapli.aisafe.enginemodel.repositories;

import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

/**
 * Repository for EngineModel aggregate.
 * US056.
 */
public interface EngineModelRepository extends DomainRepository<EngineModelCode, EngineModel> {

    /**
     * US056: engine name + manufacturer must be unique.
     * Returns the existing engine model if found (for duplicate check in controller).
     */
    Optional<EngineModel> findByNameAndManufacturer(String engineName, String manufacturerName);
}
