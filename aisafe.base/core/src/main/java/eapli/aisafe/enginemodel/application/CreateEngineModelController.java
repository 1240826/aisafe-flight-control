package eapli.aisafe.enginemodel.application;

import eapli.aisafe.aircraftmodel.domain.MotorizationType;
import eapli.aisafe.enginemodel.domain.EngineModel;
import eapli.aisafe.enginemodel.domain.EngineModelCode;
import eapli.aisafe.enginemodel.domain.EngineName;
import eapli.aisafe.enginemodel.domain.Power;
import eapli.aisafe.enginemodel.domain.Thrust;
import eapli.aisafe.enginemodel.domain.TSFC;
import eapli.aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.aisafe.manufacturer.repositories.ManufacturerRepository;
import eapli.aisafe.usermanagement.domain.AISafeRoles;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller for US056 — Create Aircraft Engine Model.
 * Actor: BackOffice Operator.
 *
 * US056 invariant: engineName + manufacturerName must be unique.
 */
@UseCaseController
public class CreateEngineModelController {

    private final AuthorizationService authz;
    private final EngineModelRepository repo;
    private final ManufacturerRepository manufacturerRepo;

    /** Production constructor — uses framework registries. */
    public CreateEngineModelController() {
        this(AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().engineModels(),
                PersistenceContext.repositories().manufacturers());
    }

    /** Testing constructor — allows injecting mocks. */
    CreateEngineModelController(final AuthorizationService authz,
                                final EngineModelRepository repo,
                                final ManufacturerRepository manufacturerRepo) {
        this.authz = authz;
        this.repo = repo;
        this.manufacturerRepo = manufacturerRepo;
    }

    /**
     * Create and persist a new EngineModel.
     * Enforces US056: engineName + manufacturerName must be unique.
     *
     * @param code              unique engine model code
     * @param engineName        engine name
     * @param manufacturerName  manufacturer name (cross-aggregate ref by name)
     * @param fuelType          fuel type (from FuelType.ALL list)
     * @param motorizationType  motorization type
     * @param powerValue        rated power value (> 0)
     * @param powerUnit         rated power unit (e.g. kW)
     * @param staticThrustValue static (take-off) thrust value (> 0)
     * @param thrustUnit        thrust unit (e.g. kN)
     * @param cruiseThrustValue cruise thrust value (> 0)
     * @param tsfcValue         TSFC value (> 0)
     * @param tsfcUnit          TSFC unit (e.g. g/kN/s)
     * @return the saved EngineModel
     * @throws IllegalArgumentException if engineName + manufacturerName combination already exists
     */
    public EngineModel createEngineModel(final String code, final String engineName,
                                          final String manufacturerName,
                                          final String fuelType, final MotorizationType motorizationType,
                                          final double powerValue, final String powerUnit,
                                          final double staticThrustValue, final String thrustUnit,
                                          final double cruiseThrustValue,
                                          final double tsfcValue, final String tsfcUnit) {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        // US056 uniqueness check: engineName + manufacturer must be unique
        if (repo.findByNameAndManufacturer(engineName, manufacturerName).isPresent()) {
            throw new IllegalArgumentException(
                    "An engine model with name '" + engineName
                    + "' and manufacturer '" + manufacturerName + "' already exists");
        }

        final EngineModel engine = new EngineModel(
                EngineModelCode.valueOf(code),
                EngineName.valueOf(engineName),
                manufacturerName,
                fuelType,
                motorizationType,
                new Power(powerValue, powerUnit),
                new Thrust(staticThrustValue, thrustUnit, "static"),
                new Thrust(cruiseThrustValue, thrustUnit, "cruise"),
                new TSFC(tsfcValue, tsfcUnit));

        return repo.save(engine);
    }

    public Iterable<EngineModel> allEngineModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return repo.findAll();
    }

    public Iterable<Manufacturer> allManufacturers() {
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);
        return manufacturerRepo.findAll();
    }

    public MotorizationType[] motorizationTypes() {
        return MotorizationType.values();
    }
}
