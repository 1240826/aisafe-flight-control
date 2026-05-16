package eapli.aisafe.simulation.repositories;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.simulation.domain.Simulation;
import eapli.aisafe.simulation.domain.ValidationResult;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository for Simulation aggregate.
 */
public interface SimulationRepository extends DomainRepository<Long, Simulation> {

    /** Find all simulations for the given Air Control Area. */
    Iterable<Simulation> findByAreaCode(AreaCode areaCode);

    /** Find all simulations with the given validation result. */
    Iterable<Simulation> findByValidationResult(ValidationResult result);
}
