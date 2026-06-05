package eapli.aisafe.persistence.jpa;

import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.domain.CompanyICAO;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.infrastructure.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of AirTransportCompanyRepository.
 */
public class JpaAirTransportCompanyRepository
        extends JpaAutoTxRepository<AirTransportCompany, CompanyIATA, CompanyIATA>
        implements AirTransportCompanyRepository {

    public JpaAirTransportCompanyRepository(final TransactionalContext autoTx) {
        super(autoTx, "iata");
    }

    public JpaAirTransportCompanyRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "iata");
    }

    @Override
    public Optional<AirTransportCompany> findByIcao(final CompanyICAO icao) {
        final Map<String, Object> params = new HashMap<>();
        params.put("icao", icao.toString());
        return matchOne("e.icao.icaoCode = :icao", params);
    }

    @Override
    public Optional<AirTransportCompany> findByName(final String name) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        return matchOne("UPPER(e.name) = UPPER(:name)", params);
    }
}
