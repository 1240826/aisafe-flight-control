package eapli.aisafe.persistence.jpa;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.infrastructure.Application;
import eapli.aisafe.simulation.domain.Simulation;
import eapli.aisafe.simulation.domain.ValidationResult;
import eapli.aisafe.simulation.repositories.SimulationRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

/**
 * JPA implementation of SimulationRepository.
 */
public class JpaSimulationRepository
        extends JpaAutoTxRepository<Simulation, Long, Long>
        implements SimulationRepository {

    public JpaSimulationRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaSimulationRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "id");
    }

    @Override
    public Iterable<Simulation> findByAreaCode(final AreaCode areaCode) {
        return match("e.areaCode.code = '" + areaCode.toString() + "'");
    }

    @Override
    public Iterable<Simulation> findByValidationResult(final ValidationResult result) {
        return match("e.validationResult = '" + result.name() + "'");
    }
}
