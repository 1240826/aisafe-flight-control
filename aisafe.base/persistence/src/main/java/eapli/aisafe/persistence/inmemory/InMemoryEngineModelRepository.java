package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.Optional;
import java.util.stream.StreamSupport;

public class InMemoryEngineModelRepository
        extends InMemoryDomainRepository<EngineModel, EngineModelCode>
        implements EngineModelRepository {

    @Override
    public Optional<EngineModel> findByNameAndManufacturer(final String engineName,
                                                            final String manufacturerName) {
        return StreamSupport.stream(findAll().spliterator(), false)
                .filter(e -> e.engineName().toString().equalsIgnoreCase(engineName)
                        && e.manufacturerName().equalsIgnoreCase(manufacturerName))
                .findFirst();
    }
}
