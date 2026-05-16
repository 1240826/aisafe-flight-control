package eapli.aisafe.persistence.jpa;

import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.aisafe.infrastructure.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.Optional;

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

    @Override
    public Optional<EngineModel> findByNameAndManufacturer(final String engineName,
                                                            final String manufacturerName) {
        return matchOne(
                "UPPER(e.engineName.name) = UPPER('" + engineName.replace("'", "''") + "')"
                + " AND UPPER(e.manufacturerName) = UPPER('"
                + manufacturerName.replace("'", "''") + "')");
    }
}
