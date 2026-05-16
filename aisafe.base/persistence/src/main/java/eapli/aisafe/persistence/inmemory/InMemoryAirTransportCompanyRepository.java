package eapli.aisafe.persistence.inmemory;

import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.domain.CompanyICAO;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.Optional;

public class InMemoryAirTransportCompanyRepository
        extends InMemoryDomainRepository<AirTransportCompany, CompanyIATA>
        implements AirTransportCompanyRepository {

    @Override
    public Optional<AirTransportCompany> findByIcao(final CompanyICAO icao) {
        return matchOne(c -> c.icao().equals(icao));
    }

    @Override
    public Optional<AirTransportCompany> findByName(final String name) {
        return matchOne(c -> c.name().equalsIgnoreCase(name));
    }
}
