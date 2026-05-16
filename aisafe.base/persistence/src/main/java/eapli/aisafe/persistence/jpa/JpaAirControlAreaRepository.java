package eapli.aisafe.persistence.jpa;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.infrastructure.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.Optional;

/**
 * JPA implementation of AirControlAreaRepository.
 */
public class JpaAirControlAreaRepository
        extends JpaAutoTxRepository<AirControlArea, AreaCode, AreaCode>
        implements AirControlAreaRepository {

    public JpaAirControlAreaRepository(final TransactionalContext autoTx) {
        super(autoTx, "code");
    }

    public JpaAirControlAreaRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "code");
    }

    @Override
    public Optional<AirControlArea> findByName(final String name) {
        return matchOne("UPPER(e.name.name) = UPPER('" + name.replace("'", "''") + "')");
    }

    @Override
    public Iterable<AirControlArea> findOverlapping(final double minLat, final double maxLat,
                                                     final double minLon, final double maxLon) {
        return match("e.boundary.maxLat >= " + minLat + " AND e.boundary.minLat <= " + maxLat
                + " AND e.boundary.maxLon >= " + minLon + " AND e.boundary.minLon <= " + maxLon);
    }
}
