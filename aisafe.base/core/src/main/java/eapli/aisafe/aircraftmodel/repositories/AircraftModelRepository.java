package eapli.aisafe.aircraftmodel.repositories;

import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository for AircraftModel aggregate.
 * US055, US057, US058.
 */
public interface AircraftModelRepository extends DomainRepository<AircraftModelCode, AircraftModel> {
}
