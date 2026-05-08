package eapli.aisafe.company.application;

import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.domain.CompanyICAO;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US060 — Register Air Transport Company.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class RegisterAirTransportCompanyController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AirTransportCompanyRepository repo =
            PersistenceContext.repositories().airTransportCompanies();

    /**
     * Register a new Air Transport Company.
     *
     * @param iata 2-letter IATA code
     * @param icao 2-3 letter ICAO code
     * @param name company name
     * @return the saved AirTransportCompany
     */
    public AirTransportCompany registerCompany(final String iata, final String icao, final String name) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final AirTransportCompany company = new AirTransportCompany(
                CompanyIATA.valueOf(iata),
                CompanyICAO.valueOf(icao),
                name);

        return repo.save(company);
    }

    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.ATC_COLLABORATOR);
        return repo.findAll();
    }
}
