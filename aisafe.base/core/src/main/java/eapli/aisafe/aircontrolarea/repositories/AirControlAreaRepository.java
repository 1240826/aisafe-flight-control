package eapli.aisafe.aircontrolarea.repositories;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

/**
 * Repository for AirControlArea aggregate.
 * US050.
 */
public interface AirControlAreaRepository extends DomainRepository<AreaCode, AirControlArea> {

    /**
     * Find by commonly-used area name (unique, case-insensitive).
     */
    Optional<AirControlArea> findByName(String name);

    /**
     * Find ACAs whose boundary overlaps with the given rectangle.
     */
    Iterable<AirControlArea> findOverlapping(double minLat, double maxLat, double minLon, double maxLon);
}
