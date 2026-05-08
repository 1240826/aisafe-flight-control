package eapli.aisafe.aircraftmodel.application;

import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US057 — Add Engine Variant to Aircraft Model.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class AddEngineVariantController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftModelRepository aircraftModelRepo =
            PersistenceContext.repositories().aircraftModels();
    private final EngineModelRepository engineModelRepo =
            PersistenceContext.repositories().engineModels();

    public Iterable<AircraftModel> allAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return aircraftModelRepo.findAll();
    }

    /**
     * Add a new engine variant (engine + motorizationType) to an existing AircraftModel.
     */
    public AircraftModel addVariant(final String aircraftModelCode,
                                     final String engineModelCode,
                                     final MotorizationType motorizationType) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final AircraftModel model = aircraftModelRepo.ofIdentity(AircraftModelCode.valueOf(aircraftModelCode))
                .orElseThrow(() -> new IllegalArgumentException("Aircraft model not found: " + aircraftModelCode));

        model.addVariant(EngineModelCode.valueOf(engineModelCode), motorizationType);
        return aircraftModelRepo.save(model);
    }
}
