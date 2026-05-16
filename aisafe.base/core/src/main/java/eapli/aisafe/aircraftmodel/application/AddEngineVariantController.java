package eapli.aisafe.aircraftmodel.application;

import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US057 — Add Engine Variant to Aircraft Model.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class AddEngineVariantController {

    private final AuthorizationService authz;
    private final AircraftModelRepository aircraftModelRepo;
    private final EngineModelRepository engineModelRepo;

    /** Production constructor — uses framework registries. */
    public AddEngineVariantController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().engineModels());
    }

    /** Testing constructor — allows injecting mocks. */
    AddEngineVariantController(final AuthorizationService authz,
                               final AircraftModelRepository aircraftModelRepo,
                               final EngineModelRepository engineModelRepo) {
        this.authz = authz;
        this.aircraftModelRepo = aircraftModelRepo;
        this.engineModelRepo = engineModelRepo;
    }

    public Iterable<AircraftModel> allAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return aircraftModelRepo.findAll();
    }

    public Iterable<EngineModel> allEngineModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return engineModelRepo.findAll();
    }

    /**
     * Add a new engine variant (engine + motorizationType) to an existing AircraftModel.
     */
    public AircraftModel addVariant(final String aircraftModelCode,
                                     final String engineModelCode,
                                     final MotorizationType motorizationType) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final AircraftModel model = aircraftModelRepo.ofIdentity(AircraftModelCode.valueOf(aircraftModelCode))
                .orElseThrow(() -> new IllegalArgumentException("Aircraft model not found: " + aircraftModelCode));

        model.addVariant(EngineModelCode.valueOf(engineModelCode), motorizationType);
        return aircraftModelRepo.save(model);
    }
}
