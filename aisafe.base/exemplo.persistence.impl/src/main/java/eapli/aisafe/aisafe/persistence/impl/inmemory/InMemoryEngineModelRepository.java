package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryEngineModelRepository
        extends InMemoryDomainRepository<EngineModel, EngineModelCode>
        implements EngineModelRepository {
}
