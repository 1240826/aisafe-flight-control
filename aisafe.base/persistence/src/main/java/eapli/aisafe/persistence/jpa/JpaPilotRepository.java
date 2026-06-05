package eapli.aisafe.persistence.jpa;

import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.infrastructure.Application;
import eapli.aisafe.pilot.domain.Pilot;
import eapli.aisafe.pilot.domain.PilotId;
import eapli.aisafe.pilot.repositories.PilotRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of PilotRepository.
 * US075, US076, US077.
 */
public class JpaPilotRepository
        extends JpaAutoTxRepository<Pilot, PilotId, PilotId>
        implements PilotRepository {

    public JpaPilotRepository(final TransactionalContext autoTx) {
        super(autoTx, "pilotId");
    }

    public JpaPilotRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "pilotId");
    }

    @Override
    public Optional<Pilot> findByLicenseNumber(final PilotId pilotId) {
        return ofIdentity(pilotId);
    }

    @Override
    public Iterable<Pilot> findByCompany(final CompanyIATA company) {
        final Map<String, Object> params = new HashMap<>();
        params.put("company", company.toString());
        return match("e.company.iataCode = :company", params);
    }

    @Override
    public Iterable<Pilot> findActiveByCompany(final CompanyIATA company) {
        final Map<String, Object> params = new HashMap<>();
        params.put("company", company.toString());
        return match("e.company.iataCode = :company AND e.active = true", params);
    }

    @Override
    public boolean hasAssignedFlights(final PilotId pilotId) {
        // Query Flight table — this entity is a different aggregate, so we need EntityManager
        final var query = entityManager().createQuery(
                "SELECT COUNT(f) FROM Flight f WHERE f.pilotLicense.licenseNumber = :license",
                Long.class);
        query.setParameter("license", pilotId.licenseNumber());
        return query.getSingleResult() > 0;
    }
}
