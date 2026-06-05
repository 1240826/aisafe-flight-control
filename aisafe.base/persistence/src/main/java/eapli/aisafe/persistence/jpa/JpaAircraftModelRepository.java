package eapli.aisafe.persistence.jpa;

import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.infrastructure.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    @Override
    public Optional<AircraftModel> findByNameAndManufacturer(final String name, final String manufacturerName) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", name.toLowerCase());
        params.put("manufacturer", manufacturerName.toLowerCase());
        return matchOne("LOWER(e.name) = :name AND LOWER(e.manufacturerName) = :manufacturer", params);
    }
}
