package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.simulation.domain.Simulation;
import eapli.aisafe.simulation.domain.ValidationResult;
import eapli.aisafe.simulation.repositories.SimulationRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainAutoNumberRepository;

/**
 * In-memory implementation of SimulationRepository.
 */
public class InMemorySimulationRepository
        extends InMemoryDomainAutoNumberRepository<Simulation>
        implements SimulationRepository {

    @Override
    public Iterable<Simulation> findByAreaCode(final AreaCode areaCode) {
        return match(s -> s.areaCode().equals(areaCode));
    }

    @Override
    public Iterable<Simulation> findByValidationResult(final ValidationResult result) {
        return match(s -> s.validationResult() == result);
    }
}
