package eapli.aisafe.manufacturer.application;

import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.aisafe.manufacturer.domain.ManufacturerName;
import eapli.aisafe.manufacturer.repositories.ManufacturerRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for registering a Manufacturer.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class RegisterManufacturerController {

    private final AuthorizationService authz;
    private final ManufacturerRepository repo;

    /** Production constructor — uses framework registries. */
    public RegisterManufacturerController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().manufacturers());
    }

    /** Testing constructor — allows injecting mocks. */
    RegisterManufacturerController(final AuthorizationService authz, final ManufacturerRepository repo) {
        this.authz = authz;
        this.repo = repo;
    }

    public Manufacturer registerManufacturer(final String name, final String country) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.save(new Manufacturer(name, country));
    }

    public Iterable<Manufacturer> allManufacturers() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.findAll();
    }

    public void removeManufacturer(final String name) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        repo.deleteOfIdentity(new ManufacturerName(name));
    }
}
