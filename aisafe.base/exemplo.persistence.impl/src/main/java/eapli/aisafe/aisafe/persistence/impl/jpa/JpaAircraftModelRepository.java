package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.exemplo.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

/**
 * JPA implementation of AircraftModelRepository.
 */
public class JpaAircraftModelRepository
        extends JpaAutoTxRepository<AircraftModel, AircraftModelCode, AircraftModelCode>
        implements AircraftModelRepository {

    public JpaAircraftModelRepository(final TransactionalContext autoTx) {
        super(autoTx, "code");
    }

    public JpaAircraftModelRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "code");
    }
}
