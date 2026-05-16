package eapli.aisafe.company.application;

import eapli.aisafe.company.domain.AirTransportCompany;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.aisafe.company.domain.CompanyICAO;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US060 — Register Air Transport Company.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class RegisterAirTransportCompanyController {

    private final AuthorizationService authz;
    private final AirTransportCompanyRepository repo;

    /** Production constructor — uses framework registries. */
    public RegisterAirTransportCompanyController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().airTransportCompanies());
    }

    /** Testing constructor — allows injecting mocks. */
    RegisterAirTransportCompanyController(final AuthorizationService authz,
                                          final AirTransportCompanyRepository repo) {
        this.authz = authz;
        this.repo = repo;
    }

    /**
     * Register a new Air Transport Company.
     *
     * @param iata 2-letter IATA code
     * @param icao 2-3 letter ICAO code
     * @param name company name
     * @return the saved AirTransportCompany
     */
    public AirTransportCompany registerCompany(final String iata, final String icao, final String name) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        if (repo.ofIdentity(CompanyIATA.valueOf(iata)).isPresent()) {
            throw new IllegalArgumentException("A company with IATA code '" + iata + "' already exists.");
        }
        if (repo.findByIcao(CompanyICAO.valueOf(icao)).isPresent()) {
            throw new IllegalArgumentException("A company with ICAO code '" + icao + "' already exists.");
        }
        if (repo.findByName(name).isPresent()) {
            throw new IllegalArgumentException("A company with name '" + name + "' already exists.");
        }

        final AirTransportCompany company = new AirTransportCompany(
                CompanyIATA.valueOf(iata),
                CompanyICAO.valueOf(icao),
                name);

        return repo.save(company);
    }

    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR,
                AISafeRoles.ATC_COLLABORATOR);
        return repo.findAll();
    }
}
