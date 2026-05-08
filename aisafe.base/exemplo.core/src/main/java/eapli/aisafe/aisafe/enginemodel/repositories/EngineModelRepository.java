package eapli.aisafe.enginemodel.repositories;

import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository for EngineModel aggregate.
 * US056.
 */
public interface EngineModelRepository extends DomainRepository<EngineModelCode, EngineModel> {
}
