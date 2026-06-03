package eapli.aisafe.persistence.jpa;

import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.aisafe.manufacturer.domain.ManufacturerName;
import eapli.aisafe.manufacturer.repositories.ManufacturerRepository;
import eapli.aisafe.infrastructure.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of ManufacturerRepository.
 */
public class JpaManufacturerRepository
        extends JpaAutoTxRepository<Manufacturer, ManufacturerName, ManufacturerName>
        implements ManufacturerRepository {

    public JpaManufacturerRepository(final TransactionalContext autoTx) {
        super(autoTx, "name");
    }

    public JpaManufacturerRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "name");
    }

    @Override
    public Optional<Manufacturer> findByNameIgnoreCase(final String name) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        return matchOne("UPPER(e.name.name) = UPPER(:name)", params);
    }
}
