package eapli.aisafe.company.repositories;

import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.domain.CompanyICAO;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

/**
 * Repository for AirTransportCompany aggregate.
 * US060.
 */
public interface AirTransportCompanyRepository extends DomainRepository<CompanyIATA, AirTransportCompany> {

    Optional<AirTransportCompany> findByIcao(CompanyICAO icao);

    Optional<AirTransportCompany> findByName(String name);
}
