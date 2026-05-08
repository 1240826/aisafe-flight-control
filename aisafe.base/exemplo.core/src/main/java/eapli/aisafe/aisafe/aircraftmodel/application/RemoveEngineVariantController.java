package eapli.aisafe.aircraftmodel.application;

import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US058 — Remove Engine Variant from Aircraft Model (Extra).
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class RemoveEngineVariantController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftModelRepository repo = PersistenceContext.repositories().aircraftModels();

    public Iterable<AircraftModel> allAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.findAll();
    }

    /**
     * Remove an engine variant from an AircraftModel by engine code.
     */
    public AircraftModel removeVariant(final String aircraftModelCode, final String engineModelCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final AircraftModel model = repo.ofIdentity(AircraftModelCode.valueOf(aircraftModelCode))
                .orElseThrow(() -> new IllegalArgumentException("Aircraft model not found: " + aircraftModelCode));

        model.removeVariant(EngineModelCode.valueOf(engineModelCode));
        return repo.save(model);
    }
}
