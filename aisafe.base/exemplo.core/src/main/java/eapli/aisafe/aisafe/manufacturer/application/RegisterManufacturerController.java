package eapli.aisafe.manufacturer.application;

import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.aisafe.manufacturer.repositories.ManufacturerRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for registering a Manufacturer.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class RegisterManufacturerController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final ManufacturerRepository repo = PersistenceContext.repositories().manufacturers();

    public Manufacturer registerManufacturer(final String name, final String country) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.save(new Manufacturer(name, country));
    }

    public Iterable<Manufacturer> allManufacturers() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.findAll();
    }
}
