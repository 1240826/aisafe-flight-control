package eapli.aisafe.aircraftmodel.application;

import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.aircraftmodel.domain.AircraftModel;
import eapli.aisafe.aircraftmodel.domain.AircraftModelCode;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US058 — Remove Engine Variant from Aircraft Model.
 * Actor: BackOffice Operator.
 *
 * Blocks removal when:
 *  - the model has only one variant remaining (domain invariant), or
 *  - any active aircraft currently uses this model (business rule).
 */
@UseCaseController
public class RemoveEngineVariantController {

    private final AuthorizationService authz;
    private final AircraftModelRepository repo;
    private final AircraftRepository aircraftRepo;

    /** Production constructor — uses framework registries. */
    public RemoveEngineVariantController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().aircraft());
    }

    /** Testing constructor — allows injecting mocks. */
    RemoveEngineVariantController(final AuthorizationService authz,
                                   final AircraftModelRepository repo,
                                   final AircraftRepository aircraftRepo) {
        this.authz = authz;
        this.repo = repo;
        this.aircraftRepo = aircraftRepo;
    }

    public Iterable<AircraftModel> allAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.findAll();
    }

    /**
     * Remove an engine variant from an AircraftModel by engine code.
     * Throws IllegalStateException if any active aircraft uses this model.
     * Throws IllegalStateException (domain invariant) if this is the last variant.
     */
    public AircraftModel removeVariant(final String aircraftModelCode, final String engineModelCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        final AircraftModelCode modelCode = AircraftModelCode.valueOf(aircraftModelCode);

        // Business rule: block if any ACTIVE aircraft is using this model
        final Iterable<eapli.aisafe.aircraft.domain.Aircraft> active =
                aircraftRepo.findActiveByAircraftModelCode(modelCode);
        if (active.iterator().hasNext()) {
            throw new IllegalStateException(
                    "Cannot modify aircraft model '" + aircraftModelCode
                    + "' — it has active aircraft assigned to it");
        }

        final AircraftModel model = repo.ofIdentity(modelCode)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft model not found: " + aircraftModelCode));

        model.removeVariant(EngineModelCode.valueOf(engineModelCode));
        return repo.save(model);
    }
}
