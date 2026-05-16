package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.Optional;

public class InMemoryAirControlAreaRepository
        extends InMemoryDomainRepository<AirControlArea, AreaCode>
        implements AirControlAreaRepository {

    @Override
    public Optional<AirControlArea> findByName(final String name) {
        return matchOne(a -> a.name().toString().equalsIgnoreCase(name));
    }

    @Override
    public Iterable<AirControlArea> findOverlapping(final double minLat, final double maxLat,
                                                     final double minLon, final double maxLon) {
        return match(a -> a.overlapsWith(minLat, maxLat, minLon, maxLon));
    }
}
