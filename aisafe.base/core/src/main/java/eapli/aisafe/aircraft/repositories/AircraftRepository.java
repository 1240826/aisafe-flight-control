package eapli.aisafe.aircraft.repositories;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

/**
 * Repository for Aircraft aggregate.
 * US070, US071, US072.
 */
public interface AircraftRepository extends DomainRepository<RegistrationNumber, Aircraft> {

    /**
     * US070: uniqueness check.
     */
    Optional<Aircraft> findByRegistrationNumber(RegistrationNumber registrationNumber);

    /**
     * US071: only ACTIVE aircraft shown for decommission selection.
     */
    Iterable<Aircraft> findAllActive();

    /**
     * US072: all aircraft (ACTIVE + DECOMMISSIONED) for a company.
     */
    Iterable<Aircraft> findByCompanyId(CompanyIATA companyId);

    /**
     * US058: find active aircraft that use a given model (blocks variant removal when in use).
     */
    Iterable<Aircraft> findActiveByAircraftModelCode(AircraftModelCode modelCode);

    /**
     * US072a: filter by model.
     */
    Iterable<Aircraft> findByCompanyIdAndModel(CompanyIATA companyId, AircraftModelCode modelCode);

    /**
     * US072b: filter by maker name.
     */
    Iterable<Aircraft> findByCompanyIdAndMaker(CompanyIATA companyId, String makerName);

    /**
     * US072c: filter by minimum capacity.
     */
    Iterable<Aircraft> findByCompanyIdAndMinCapacity(CompanyIATA companyId, int minCapacity);
}
