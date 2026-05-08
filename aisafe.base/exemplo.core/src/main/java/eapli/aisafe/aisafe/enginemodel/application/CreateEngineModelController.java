package eapli.aisafe.enginemodel.application;

import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.enginemodel.domain.EngineName;
import eapli.aisafe.enginemodel.domain.Power;
import eapli.aisafe.enginemodel.domain.Thrust;
import eapli.aisafe.enginemodel.domain.TSFC;
import eapli.aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US056 — Create Aircraft Engine Model.
 * Actor: Admin / BackOffice Operator.
 */
@UseCaseController
public class CreateEngineModelController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final EngineModelRepository repo = PersistenceContext.repositories().engineModels();

    /**
     * Create and persist a new EngineModel.
     */
    public EngineModel createEngineModel(final String code, final String engineName,
                                          final String fuelType, final MotorizationType motorizationType,
                                          final double powerValue, final String powerUnit,
                                          final double staticThrustValue, final String thrustUnit,
                                          final double cruiseThrustValue,
                                          final double tsfcValue, final String tsfcUnit) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);

        final EngineModel engine = new EngineModel(
                EngineModelCode.valueOf(code),
                EngineName.valueOf(engineName),
                fuelType,
                motorizationType,
                new Power(powerValue, powerUnit),
                new Thrust(staticThrustValue, thrustUnit, "static"),
                new Thrust(cruiseThrustValue, thrustUnit, "cruise"),
                new TSFC(tsfcValue, tsfcUnit));

        return repo.save(engine);
    }

    public Iterable<EngineModel> allEngineModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.ADMIN, AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.findAll();
    }

    public MotorizationType[] motorizationTypes() {
        return MotorizationType.values();
    }
}
