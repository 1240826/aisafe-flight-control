package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.exemplo.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

/**
 * JPA implementation of EngineModelRepository.
 */
public class JpaEngineModelRepository
        extends JpaAutoTxRepository<EngineModel, EngineModelCode, EngineModelCode>
        implements EngineModelRepository {

    public JpaEngineModelRepository(final TransactionalContext autoTx) {
        super(autoTx, "code");
    }

    public JpaEngineModelRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "code");
    }
}
