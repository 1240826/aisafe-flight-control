package eapli.aisafe.persistence.jpa;

import eapli.aisafe.aircraft.domain.Aircraft;
import eapli.aisafe.aircraft.domain.OperationalStatus;
import eapli.aisafe.aircraft.domain.RegistrationNumber;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.infrastructure.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.Optional;

/**
 * JPA implementation of AircraftRepository.
 */
public class JpaAircraftRepository
        extends JpaAutoTxRepository<Aircraft, RegistrationNumber, RegistrationNumber>
        implements AircraftRepository {

    public JpaAircraftRepository(final TransactionalContext autoTx) {
        super(autoTx, "registrationNumber");
    }

    public JpaAircraftRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "registrationNumber");
    }

    @Override
    public Optional<Aircraft> findByRegistrationNumber(final RegistrationNumber registrationNumber) {
        return findById(registrationNumber);
    }

    @Override
    public Iterable<Aircraft> findAllActive() {
        return match("e.operationalStatus = eapli.aisafe.aircraft.domain.OperationalStatus.ACTIVE");
    }

    @Override
    public Iterable<Aircraft> findByCompanyId(final CompanyIATA companyId) {
        return match("e.companyId.iataCode = '" + companyId.toString() + "'");
    }

    @Override
    public Iterable<Aircraft> findActiveByAircraftModelCode(final AircraftModelCode modelCode) {
        return match("e.aircraftModelCode.code = '" + modelCode.toString() + "'"
                + " AND e.operationalStatus = eapli.aisafe.aircraft.domain.OperationalStatus.ACTIVE");
    }
}
